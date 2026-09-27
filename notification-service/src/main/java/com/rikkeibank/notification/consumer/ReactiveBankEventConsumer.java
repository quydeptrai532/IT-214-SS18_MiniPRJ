package com.rikkeibank.notification.consumer;

import com.rikkeibank.common.event.BankEvent;
import com.rikkeibank.common.event.KafkaTopics;
import com.rikkeibank.notification.service.NotificationStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.List;
import java.util.Map;

/**
 * ⭐ CONSUMER KAFKA KIỂU REACTIVE (WebFlux + reactor-kafka).
 *
 * So sánh với @KafkaListener (blocking):
 *   - @KafkaListener: mỗi message chiếm một thread của container, xử lý tuần tự.
 *   - KafkaReceiver : trả về Flux<ReceiverRecord> -> xử lý như một luồng dữ liệu,
 *     hỗ trợ backpressure, không chặn thread, dễ ghép (compose) với các toán tử reactive khác.
 *
 * Consumer group riêng để không tranh message với các consumer khác.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveBankEventConsumer {

    public static final String GROUP_ID = "notification-service-reactive";

    private final ConsumerFactory<String, BankEvent> consumerFactory;
    private final NotificationStore notificationStore;

    private Disposable subscription;

    @PostConstruct
    public void start() {
        Map<String, Object> properties = consumerFactory.getConfigurationProperties();
        ReceiverOptions<String, BankEvent> options = ReceiverOptions.<String, BankEvent>create(properties)
                .consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID)
                .consumerProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .subscription(List.of(KafkaTopics.TRANSACTION_EVENTS, KafkaTopics.ACCOUNT_EVENTS));

        this.subscription = KafkaReceiver.create(options)
                .receive()
                .doOnNext(record -> log.debug("[REACTIVE] Nhận event {} offset={}",
                        record.value().getType(), record.offset()))
                // Xử lý mỗi sự kiện: tạo thông báo (ở đây là thao tác nhanh, không chặn)
                .doOnNext(record -> notificationStore.save(record.value()))
                // Cam kết offset SAU KHI xử lý xong -> không mất sự kiện nếu service chết
                .doOnNext(record -> record.receiverOffset().acknowledge())
                .onErrorContinue((error, record) ->
                        log.error("[REACTIVE] Bỏ qua sự kiện lỗi: {}", error.getMessage()))
                .subscribe(
                        null,
                        error -> log.error("[REACTIVE] Luồng Kafka dừng: {}", error.getMessage(), error),
                        () -> log.info("[REACTIVE] Luồng Kafka kết thúc"));

        log.info("[REACTIVE] Đã khởi động consumer reactive (group={}) lắng nghe topic {} và {}",
                GROUP_ID, KafkaTopics.TRANSACTION_EVENTS, KafkaTopics.ACCOUNT_EVENTS);
    }

    @PreDestroy
    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("[REACTIVE] Đã dừng consumer reactive");
        }
    }
}
