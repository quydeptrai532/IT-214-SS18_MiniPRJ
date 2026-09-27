package com.rikkeibank.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Sự kiện dùng chung cho toàn hệ thống (trao đổi qua Kafka).
 * Một cấu trúc duy nhất cho mọi loại sự kiện giúp consumer dễ xử lý và mở rộng.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankEvent implements Serializable {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private BankEventType type;

    private Long transactionId;
    private Long fromAccountId;
    private Long toAccountId;
    private Long customerId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String currency;
    private String status;
    private String message;

    @Builder.Default
    private Instant occurredAt = Instant.now();
}
