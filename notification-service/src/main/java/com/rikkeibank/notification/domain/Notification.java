package com.rikkeibank.notification.domain;

import com.rikkeibank.common.event.BankEvent;
import com.rikkeibank.common.event.BankEventType;

import java.time.Instant;
import java.util.UUID;

/** Thông báo sinh ra từ sự kiện Kafka. */
public record Notification(String id, String title, String content, String level,
                           Long transactionId, Long customerId, BankEventType eventType, Instant createdAt) {

    public static Notification from(BankEvent event) {
        return new Notification(
                UUID.randomUUID().toString(),
                titleFor(event),
                contentFor(event),
                levelFor(event),
                event.getTransactionId(),
                event.getCustomerId(),
                event.getType(),
                Instant.now());
    }

    private static String titleFor(BankEvent event) {
        return switch (event.getType()) {
            case TRANSFER_DEBITED -> "Giao dịch chuyển khoản đã được khởi tạo";
            case TRANSFER_COMPLETED -> "Chuyển khoản thành công";
            case TRANSFER_FAILED -> "Chuyển khoản thất bại";
            case TRANSFER_COMPENSATED -> "Đã hoàn tiền";
            case ACCOUNT_BALANCE_CHANGED -> "Biến động số dư";
            case ACCOUNT_STATUS_CHANGED -> "Trạng thái tài khoản thay đổi";
            case USER_FORCE_LOGOUT -> "Phiên đăng nhập bị thu hồi";
        };
    }

    private static String contentFor(BankEvent event) {
        StringBuilder builder = new StringBuilder();
        if (event.getAmount() != null) {
            builder.append("Số tiền: ").append(event.getAmount()).append(' ').append(event.getCurrency()).append(". ");
        }
        if (event.getBalanceAfter() != null) {
            builder.append("Số dư sau giao dịch: ").append(event.getBalanceAfter()).append(". ");
        }
        if (event.getMessage() != null) {
            builder.append(event.getMessage());
        }
        return builder.toString().trim();
    }

    private static String levelFor(BankEvent event) {
        return switch (event.getType()) {
            case TRANSFER_FAILED -> "ERROR";
            case TRANSFER_COMPENSATED -> "WARN";
            default -> "INFO";
        };
    }
}
