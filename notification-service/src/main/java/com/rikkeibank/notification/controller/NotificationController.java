package com.rikkeibank.notification.controller;

import com.rikkeibank.common.security.AuthUser;
import com.rikkeibank.notification.domain.Notification;
import com.rikkeibank.notification.service.NotificationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationStore notificationStore;

    /** Khách hàng chỉ nhận thông báo của chính mình. */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Flux<Notification> myNotifications(@AuthenticationPrincipal AuthUser user) {
        return notificationStore.findByCustomer(user == null ? null : user.userId());
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER') or @notificationOwnershipChecker.isOwner(authentication, #customerId)")
    public Flux<Notification> byCustomer(@org.springframework.web.bind.annotation.PathVariable Long customerId) {
        return notificationStore.findByCustomer(customerId);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<Notification> all() {
        return notificationStore.findAll();
    }

    @GetMapping("/count")
    public Mono<Map<String, Integer>> count() {
        return Mono.just(Map.of("totalNotifications", notificationStore.count()));
    }
}
