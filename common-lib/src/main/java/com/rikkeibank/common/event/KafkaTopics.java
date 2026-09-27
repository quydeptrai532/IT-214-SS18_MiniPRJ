package com.rikkeibank.common.event;

/** Tên các topic Kafka của hệ thống RikkeiBank. */
public final class KafkaTopics {

    /** Sự kiện giao dịch chuyển khoản (Saga + event-driven). */
    public static final String TRANSACTION_EVENTS = "rikkeibank.transaction.events";

    /** Sự kiện biến động số dư tài khoản (choreography). */
    public static final String ACCOUNT_EVENTS = "rikkeibank.account.events";

    private KafkaTopics() {
    }
}
