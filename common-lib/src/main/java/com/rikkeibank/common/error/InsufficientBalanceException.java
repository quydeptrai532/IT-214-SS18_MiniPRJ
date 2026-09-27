package com.rikkeibank.common.error;

/** Số dư không đủ để thực hiện giao dịch -> HTTP 422. */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
