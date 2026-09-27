package com.rikkeibank.account.service;

import com.rikkeibank.common.event.BankEvent;
import com.rikkeibank.common.event.BankEventType;
import com.rikkeibank.common.event.KafkaTopics;
import com.rikkeibank.account.client.CustomerClient;
import com.rikkeibank.account.dto.AccountDtos.AccountResponse;
import com.rikkeibank.account.dto.AccountDtos.BalanceResponse;
import com.rikkeibank.account.dto.AccountDtos.CreateAccountRequest;
import com.rikkeibank.account.dto.AccountDtos.UpdateAccountRequest;
import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.repository.AccountRepository;
import com.rikkeibank.common.error.BusinessException;
import com.rikkeibank.common.error.InsufficientBalanceException;
import com.rikkeibank.common.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Nghiệp vụ tài khoản.
 *
 * Cache-Aside:
 *   getAccountById  -> @Cacheable (lần đầu query DB, sau đó lấy Redis)
 *   updateAccount   -> @CachePut  (cập nhật lại cache)
 *   delete/debit/credit -> @CacheEvict (số dư đổi => bắt buộc xoá cache)
 *
 * Kafka: mỗi lần số dư thay đổi đều phát sự kiện ACCOUNT_BALANCE_CHANGED
 * (notification-service và các consumer khác phản ứng độc lập -> loose coupling).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerClient customerClient;
    private final KafkaTemplate<String, BankEvent> kafkaTemplate;

    // ================== ĐỌC ==================

    @Cacheable(cacheNames = "accounts", key = "#id")
    public AccountResponse getAccountById(Long id) {
        log.info("Querying DB for account id={}", id);
        return toResponse(find(id));
    }

    @Cacheable(cacheNames = "account-list", key = "#customerId")
    public List<AccountResponse> getAccountsByCustomer(Long customerId) {
        log.info("Querying DB for accounts of customerId={}", customerId);
        return accountRepository.findByCustomerId(customerId).stream().map(this::toResponse).toList();
    }

    public BalanceResponse getBalance(Long id) {
        Account account = find(id);
        return new BalanceResponse(account.getId(), account.getAccountNumber(), account.getBalance(), account.getCurrency());
    }

    public List<AccountResponse> getAll() {
        return accountRepository.findAll().stream().map(this::toResponse).toList();
    }

    // ================== TẠO / SỬA / XOÁ ==================

    @Transactional
    @CacheEvict(cacheNames = "account-list", allEntries = true)
    public AccountResponse createAccount(CreateAccountRequest request) {
        Object customer = customerClient.getCustomer(request.customerId());
        if (customer == null) {
            throw new BusinessException("CUSTOMER_UNAVAILABLE",
                    "Không xác minh được khách hàng id=" + request.customerId() + " (customer-service không khả dụng)");
        }
        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .customerId(request.customerId())
                .accountTypeId(request.accountTypeId())
                .balance(request.initialBalance() == null ? BigDecimal.ZERO : request.initialBalance())
                .currency(request.currency() == null ? "VND" : request.currency())
                .status("ACTIVE")
                .build();
        Account saved = accountRepository.save(account);
        log.info("[ACCOUNT] Đã mở tài khoản id={} số={} cho customerId={}",
                saved.getId(), saved.getAccountNumber(), saved.getCustomerId());
        return toResponse(saved);
    }

    @Transactional
    @Caching(put = @CachePut(cacheNames = "accounts", key = "#id"),
            evict = @CacheEvict(cacheNames = "account-list", allEntries = true))
    public AccountResponse updateAccount(Long id, UpdateAccountRequest request) {
        Account account = find(id);
        if (request.status() != null) {
            account.setStatus(request.status().toUpperCase());
        }
        if (request.accountTypeId() != null) {
            account.setAccountTypeId(request.accountTypeId());
        }
        log.info("[ACCOUNT] Cập nhật tài khoản id={}", id);
        return toResponse(accountRepository.save(account));
    }

    /** ADMIN khoá/mở khoá tài khoản. */
    @Transactional
    @Caching(put = @CachePut(cacheNames = "accounts", key = "#id"),
            evict = @CacheEvict(cacheNames = "account-list", allEntries = true))
    public AccountResponse changeStatus(Long id, String status) {
        Account account = find(id);
        account.setStatus(status.toUpperCase());
        log.warn("[ACCOUNT] Đổi trạng thái tài khoản id={} -> {}", id, account.getStatus());
        Account saved = accountRepository.save(account);
        publishEvent(saved, BankEventType.ACCOUNT_STATUS_CHANGED, null, "Trạng thái: " + saved.getStatus());
        return toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "accounts", key = "#id"),
            @CacheEvict(cacheNames = "account-list", allEntries = true)
    })
    public void deleteAccount(Long id) {
        Account account = find(id);
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("ACCOUNT_NOT_EMPTY", "Không thể xoá tài khoản còn số dư");
        }
        accountRepository.delete(account);
        log.warn("[ACCOUNT] Đã xoá tài khoản id={}", id);
    }

    // ================== GHI NỢ / GHI CÓ (Saga gọi vào) ==================

    /** Trừ tiền - bước 1 của Saga chuyển khoản. */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "accounts", key = "#accountId"),
            @CacheEvict(cacheNames = "account-list", allEntries = true)
    })
    public BalanceResponse debit(Long accountId, BigDecimal amount, Long transactionId) {
        Account account = find(accountId);
        requireActive(account);
        if (account.getBalance().compareTo(amount) < 0) {
            log.error("[ACCOUNT] Tài khoản {} không đủ số dư: có {} - cần {}",
                    account.getAccountNumber(), account.getBalance(), amount);
            throw new InsufficientBalanceException("Tài khoản " + account.getAccountNumber()
                    + " không đủ số dư (hiện có " + account.getBalance() + ")");
        }
        account.setBalance(account.getBalance().subtract(amount));
        Account saved = accountRepository.save(account);
        log.info("[ACCOUNT] GHI NỢ {} {} -> số dư còn {}", amount, saved.getAccountNumber(), saved.getBalance());
        publishEvent(saved, BankEventType.ACCOUNT_BALANCE_CHANGED, transactionId, "Ghi nợ " + amount);
        return new BalanceResponse(saved.getId(), saved.getAccountNumber(), saved.getBalance(), saved.getCurrency());
    }

    /** Cộng tiền - bước 2 của Saga, đồng thời dùng làm COMPENSATING khi bước sau thất bại. */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "accounts", key = "#accountId"),
            @CacheEvict(cacheNames = "account-list", allEntries = true)
    })
    public BalanceResponse credit(Long accountId, BigDecimal amount, Long transactionId) {
        Account account = find(accountId);
        requireActive(account);
        account.setBalance(account.getBalance().add(amount));
        Account saved = accountRepository.save(account);
        log.info("[ACCOUNT] GHI CÓ {} {} -> số dư mới {}", amount, saved.getAccountNumber(), saved.getBalance());
        publishEvent(saved, BankEventType.ACCOUNT_BALANCE_CHANGED, transactionId, "Ghi có " + amount);
        return new BalanceResponse(saved.getId(), saved.getAccountNumber(), saved.getBalance(), saved.getCurrency());
    }

    // ================== HELPERS ==================

    private void requireActive(Account account) {
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new BusinessException("ACCOUNT_NOT_ACTIVE",
                    "Tài khoản " + account.getAccountNumber() + " đang ở trạng thái " + account.getStatus());
        }
    }

    private Account find(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản id=" + id));
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = "9704" + ThreadLocalRandom.current().nextLong(100000000L, 999999999L);
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    private void publishEvent(Account account, BankEventType type, Long transactionId, String message) {
        BankEvent event = BankEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(type)
                .transactionId(transactionId)
                .customerId(account.getCustomerId())
                .balanceAfter(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .message(message + " (tài khoản " + account.getAccountNumber() + ")")
                .occurredAt(Instant.now())
                .build();
        kafkaTemplate.send(KafkaTopics.ACCOUNT_EVENTS, String.valueOf(account.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA] Không gửi được sự kiện {} cho tài khoản {}: {}",
                                type, account.getId(), ex.getMessage());
                    } else {
                        log.info("[KAFKA] Đã phát sự kiện {} cho tài khoản {}", type, account.getAccountNumber());
                    }
                });
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getCustomerId(), a.getAccountTypeId(),
                a.getBalance(), a.getCurrency(), a.getStatus());
    }
}
