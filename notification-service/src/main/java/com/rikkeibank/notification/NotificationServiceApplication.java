package com.rikkeibank.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * RikkeiBank - Notification Service.
 *
 * Đây là service minh hoạ GIAO TIẾP BẤT ĐỒNG BỘ kiểu EVENT-DRIVEN trên nền WebFlux:
 *   Kafka topic "rikkeibank.transaction.events" / "rikkeibank.account.events"
 *      -> KafkaReceiver (reactor-kafka) trả về Flux<ReceiverRecord>
 *      -> xử lý theo luồng dữ liệu (backpressure, non-blocking)
 *      -> lưu thông báo và phục vụ qua REST (WebFlux)
 *
 * Nhờ reactor-kafka, service không cần một thread cho mỗi message và hoàn toàn tách rời
 * (loose coupling) với service phát sự kiện.
 */
/**
 * Chỉ scan các package dùng chung CẦN THIẾT (không scan com.rikkeibank.common.config
 * vì đó là cấu hình bảo mật kiểu servlet, không dùng được cho WebFlux).
 */
@SpringBootApplication(scanBasePackages = {
        "com.rikkeibank.notification",
        "com.rikkeibank.common.security",
        "com.rikkeibank.common.error",
        "com.rikkeibank.common.aop",
        "com.rikkeibank.common.event"
})
@EnableDiscoveryClient
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
