package com.rikkeibank.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record TransferRequest(
            @NotNull Long fromAccountId,
            @NotNull Long toAccountId,
            @NotNull @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0") BigDecimal amount,
            String description,
            /** Bắt buộc để chống tạo giao dịch trùng khi client gửi lại. */
            @NotNull String idempotencyKey) {
    }

    public record TransferResponse(Long transactionId, String transactionCode, String status,
                                   BigDecimal amount, String currency, Long fromAccountId, Long toAccountId,
                                   String failureReason, LocalDateTime createdAt, LocalDateTime completedAt,
                                   List<String> sagaSteps) {
    }

    public record TransactionSummary(Long transactionId, String transactionCode, String status,
                                     BigDecimal amount, String currency, Long fromAccountId, Long toAccountId,
                                     String failureReason, LocalDateTime createdAt) {
    }
}
