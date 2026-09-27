package com.rikkeibank.transaction.service;

import com.rikkeibank.common.event.BankEvent;
import com.rikkeibank.common.event.BankEventType;
import com.rikkeibank.common.event.KafkaTopics;
import com.rikkeibank.transaction.entity.TransferTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Phát sự kiện giao dịch lên Kafka để các service khác phản ứng (event-driven). */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    private final KafkaTemplate<String, BankEvent> kafkaTemplate;

    public void publish(TransferTransaction transaction, BankEventType type, String message) {
        BankEvent event = BankEvent.builder()
                .type(type)
                .transactionId(transaction.getId())
                .fromAccountId(transaction.getFromAccountId())
                .toAccountId(transaction.getToAccountId())
                .customerId(transaction.getCreatedByUserId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .message(message)
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.TRANSACTION_EVENTS, String.valueOf(transaction.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA] Không gửi được sự kiện {} cho giao dịch {}: {}",
                                type, transaction.getTransactionCode(), ex.getMessage());
                    } else {
                        log.info("[KAFKA] -> {} | giao dịch {} | partition={} offset={}",
                                type, transaction.getTransactionCode(),
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}
