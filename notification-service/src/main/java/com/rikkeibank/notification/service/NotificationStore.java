package com.rikkeibank.notification.service;

import com.rikkeibank.common.event.BankEvent;
import com.rikkeibank.notification.domain.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lưu thông báo trong bộ nhớ và phục vụ theo kiểu reactive.
 * (Bản demo không dùng JPA để tránh trộn blocking vào luồng WebFlux;
 *  production nên dùng R2DBC hoặc DB bất đồng bộ.)
 */
@Slf4j
@Service
public class NotificationStore {

    private final Map<String, Notification> notifications = new ConcurrentHashMap<>();

    public Notification save(BankEvent event) {
        Notification notification = Notification.from(event);
        notifications.put(notification.id(), notification);
        log.info("[NOTIFICATION] {} | customerId={} | {}", notification.title(),
                notification.customerId(), notification.content());
        return notification;
    }

    public Flux<Notification> findAll() {
        return Flux.fromStream(notifications.values().stream()
                .sorted(Comparator.comparing(Notification::createdAt).reversed()));
    }

    public Flux<Notification> findByCustomer(Long customerId) {
        return findAll().filter(n -> customerId.equals(n.customerId()));
    }

    public List<Notification> snapshot() {
        return List.copyOf(notifications.values());
    }

    public int count() {
        return notifications.size();
    }
}
