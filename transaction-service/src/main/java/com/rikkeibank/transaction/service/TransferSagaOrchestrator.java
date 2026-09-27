package com.rikkeibank.transaction.service;

import com.rikkeibank.common.error.BusinessException;
import com.rikkeibank.common.event.BankEventType;
import com.rikkeibank.common.security.AuthUser;
import com.rikkeibank.transaction.client.AccountClient;
import com.rikkeibank.transaction.dto.TransactionDtos.TransactionSummary;
import com.rikkeibank.transaction.dto.TransactionDtos.TransferRequest;
import com.rikkeibank.transaction.dto.TransactionDtos.TransferResponse;
import com.rikkeibank.transaction.entity.SagaStep;
import com.rikkeibank.transaction.entity.TransferTransaction;
import com.rikkeibank.transaction.repository.TransactionRepositories.SagaStepRepository;
import com.rikkeibank.transaction.repository.TransactionRepositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ⭐ ORCHESTRATOR SAGA CHO NGHIỆP VỤ CHUYỂN KHOẢN.
 *
 * Luồng:
 *   B1. Ghi nợ tài khoản nguồn  (debit)
 *   B2. Ghi có tài khoản đích   (credit)
 *   Nếu B2 thất bại -> BÙ TRỪ: ghi có lại tài khoản nguồn (compensating) để hoàn tiền.
 *
 * Vì sao KHÔNG dùng @Transactional cho toàn bộ phương thức?
 *   Vì đây là giao dịch PHÂN TÁN: mỗi bước là một transaction riêng ở service khác.
 *   Nếu bọc một transaction lớn, khi có lỗi thì toàn bộ nhật ký Saga (đã ghi) cũng bị rollback
 *   -> mất dấu vết điều tra. Ở đây mỗi bước được ghi nhận NGAY khi hoàn thành.
 *
 * Chống trùng: mỗi yêu cầu có idempotencyKey; gửi lại cùng key sẽ trả về giao dịch cũ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferSagaOrchestrator {

    private final TransactionRepository transactionRepository;
    private final SagaStepRepository sagaStepRepository;
    private final AccountGateway accountGateway;
    private final TransactionEventProducer eventProducer;

    public TransferResponse transfer(TransferRequest request, AuthUser user) {
        // ---------- Chống tạo giao dịch trùng ----------
        Optional<TransferTransaction> duplicated = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (duplicated.isPresent()) {
            log.warn("[SAGA] Yêu cầu trùng (idempotencyKey={}) -> trả về giao dịch đã có {}",
                    request.idempotencyKey(), duplicated.get().getTransactionCode());
            return toResponse(duplicated.get());
        }

        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new BusinessException("INVALID_TRANSFER", "Tài khoản nguồn và đích phải khác nhau");
        }

        TransferTransaction tx = transactionRepository.save(TransferTransaction.builder()
                .transactionCode("TXN" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 999))
                .fromAccountId(request.fromAccountId())
                .toAccountId(request.toAccountId())
                .amount(request.amount())
                .currency("VND")
                .status(TransferTransaction.STATUS_PENDING)
                .description(request.description())
                .idempotencyKey(request.idempotencyKey())
                .createdByUserId(user == null ? null : user.userId())
                .createdByUsername(user == null ? null : user.username())
                .createdByRole(user == null ? null : user.role())
                .build());

        log.info("[SAGA] === BẮT ĐẦU chuyển khoản {} | {} -> {} | {} VND ===",
                tx.getTransactionCode(), tx.getFromAccountId(), tx.getToAccountId(), tx.getAmount());

        // ================= BƯỚC 1: GHI NỢ =================
        try {
            accountGateway.debit(tx.getFromAccountId(), tx.getAmount(), tx.getId());
            saveStep(tx, "DEBIT", "SUCCESS", "Đã trừ tiền tài khoản nguồn " + tx.getFromAccountId());
            eventProducer.publish(tx, BankEventType.TRANSFER_DEBITED, "Đã trừ tiền tài khoản nguồn");
        } catch (RuntimeException ex) {
            saveStep(tx, "DEBIT", "FAILED", ex.getMessage());
            // Chưa trừ được tiền -> không cần bù trừ, chỉ đánh dấu thất bại
            tx.setStatus(TransferTransaction.STATUS_FAILED);
            tx.setFailureReason("Ghi nợ thất bại: " + ex.getMessage());
            transactionRepository.save(tx);
            eventProducer.publish(tx, BankEventType.TRANSFER_FAILED, tx.getFailureReason());
            log.error("[SAGA] Thất bại ở bước GHI NỢ cho {}: {}", tx.getTransactionCode(), ex.getMessage());
            throw new BusinessException("TRANSFER_FAILED", tx.getFailureReason());
        }

        // ================= BƯỚC 2: GHI CÓ =================
        try {
            AccountClient.BalanceResponse target =
                    accountGateway.credit(tx.getToAccountId(), tx.getAmount(), tx.getId());
            saveStep(tx, "CREDIT", "SUCCESS", "Đã cộng tiền tài khoản đích " + tx.getToAccountId());

            tx.setStatus(TransferTransaction.STATUS_COMPLETED);
            tx.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(tx);
            eventProducer.publish(tx, BankEventType.TRANSFER_COMPLETED, "Chuyển khoản thành công");

            log.info("[SAGA] === HOÀN TẤT {} | số dư tài khoản đích: {} ===",
                    tx.getTransactionCode(), target.balance());

        } catch (RuntimeException ex) {
            saveStep(tx, "CREDIT", "FAILED", ex.getMessage());
            log.error("[SAGA-COMPENSATE] Bước GHI CÓ thất bại cho {} ({}) -> KÍCH HOẠT BÙ TRỪ",
                    tx.getTransactionCode(), ex.getMessage());

            // ================= BÙ TRỪ (COMPENSATING TRANSACTION) =================
            try {
                accountGateway.credit(tx.getFromAccountId(), tx.getAmount(), tx.getId());
                saveStep(tx, "COMPENSATE_DEBIT", "COMPENSATED",
                        "Đã hoàn " + tx.getAmount() + " cho tài khoản nguồn " + tx.getFromAccountId());

                tx.setStatus(TransferTransaction.STATUS_COMPENSATED);
                tx.setFailureReason("Ghi có thất bại: " + ex.getMessage() + " - đã hoàn tiền");
                transactionRepository.save(tx);
                eventProducer.publish(tx, BankEventType.TRANSFER_COMPENSATED,
                        "Đã hoàn tiền do ghi có thất bại");

                log.warn("[SAGA-COMPENSATE] Đã HOÀN TIỀN {} cho tài khoản {} - dữ liệu không bị du lệch",
                        tx.getAmount(), tx.getFromAccountId());

            } catch (RuntimeException compensationError) {
                saveStep(tx, "COMPENSATE_DEBIT", "FAILED", compensationError.getMessage());
                tx.setStatus(TransferTransaction.STATUS_FAILED);
                tx.setFailureReason("Ghi có thất bại VÀ hoàn tiền thất bại: " + compensationError.getMessage());
                transactionRepository.save(tx);
                eventProducer.publish(tx, BankEventType.TRANSFER_FAILED, tx.getFailureReason());
                // Trường hợp xấu nhất: cần con người can thiệp -> log CRITICAL để bảo trì
                log.error("[SAGA-COMPENSATE][CRITICAL] HOÀN TIỀN THẤT BẠI cho {} - cần xử lý thủ công!",
                        tx.getTransactionCode(), compensationError);
            }
        }

        return toResponse(tx);
    }

    public TransferResponse getById(Long id) {
        return toResponse(transactionRepository.findById(id)
                .orElseThrow(() -> new com.rikkeibank.common.error.ResourceNotFoundException(
                        "Không tìm thấy giao dịch id=" + id)));
    }

    public TransferResponse getByCode(String code) {
        return toResponse(transactionRepository.findByTransactionCode(code)
                .orElseThrow(() -> new com.rikkeibank.common.error.ResourceNotFoundException(
                        "Không tìm thấy giao dịch code=" + code)));
    }

    /** Giao dịch trong ngày - dành cho TELLER. */
    public List<TransactionSummary> getTodayTransactions() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return transactionRepository.findByCreatedAtBetween(start, end).stream().map(this::toSummary).toList();
    }

    public List<TransactionSummary> getAll() {
        return transactionRepository.findAll().stream().map(this::toSummary).toList();
    }

    public List<TransactionSummary> getByAccount(Long accountId) {
        return transactionRepository.findByFromAccountIdOrToAccountId(accountId, accountId).stream()
                .map(this::toSummary).toList();
    }

    private void saveStep(TransferTransaction tx, String stepName, String status, String message) {
        sagaStepRepository.save(SagaStep.builder()
                .transactionId(tx.getId())
                .stepName(stepName)
                .status(status)
                .message(message)
                .build());
        log.info("[SAGA-STEP] {} | {} | {} | {}", tx.getTransactionCode(), stepName, status, message);
    }

    private TransferResponse toResponse(TransferTransaction tx) {
        List<String> steps = sagaStepRepository.findByTransactionIdOrderByIdAsc(tx.getId()).stream()
                .map(step -> step.getStepName() + "=" + step.getStatus())
                .toList();
        return new TransferResponse(tx.getId(), tx.getTransactionCode(), tx.getStatus(), tx.getAmount(),
                tx.getCurrency(), tx.getFromAccountId(), tx.getToAccountId(), tx.getFailureReason(),
                tx.getCreatedAt(), tx.getCompletedAt(), steps);
    }

    private TransactionSummary toSummary(TransferTransaction tx) {
        return new TransactionSummary(tx.getId(), tx.getTransactionCode(), tx.getStatus(), tx.getAmount(),
                tx.getCurrency(), tx.getFromAccountId(), tx.getToAccountId(), tx.getFailureReason(),
                tx.getCreatedAt());
    }
}
