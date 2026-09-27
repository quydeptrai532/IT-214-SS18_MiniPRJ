package com.rikkeibank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record CreateAccountRequest(
            @NotNull Long customerId,
            @NotNull Long accountTypeId,
            @DecimalMin(value = "0.0", message = "Số dư ban đầu không được âm") BigDecimal initialBalance,
            String currency) {
    }

    public record UpdateAccountRequest(String status, Long accountTypeId) {
    }

    /** Yêu cầu ghi nợ/ghi có - dùng cho cả REST và Saga nội bộ. */
    public record BalanceChangeRequest(
            @NotNull @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0") BigDecimal amount,
            String description) {
    }

    public record AccountResponse(Long id, String accountNumber, Long customerId, Long accountTypeId,
                                  BigDecimal balance, String currency, String status) implements Serializable {
    }

    public record BalanceResponse(Long accountId, String accountNumber, BigDecimal balance, String currency) {
    }
}
