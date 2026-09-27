package com.rikkeibank.identity.config;

import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.entity.UserRole;
import com.rikkeibank.identity.entity.UserStatus;
import com.rikkeibank.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Tạo sẵn 3 tài khoản mẫu (ADMIN / TELLER / CUSTOMER) khi khởi động lần đầu. */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner initUsers() {
        return args -> {
            seed("admin", "admin123", "Quản trị viên RikkeiBank", "admin@rikkeibank.vn", UserRole.ADMIN, null);
            seed("teller01", "teller123", "Giao dịch viên 01", "teller01@rikkeibank.vn", UserRole.TELLER, null);
            seed("customer01", "customer123", "Nguyễn Văn A", "a@rikkeibank.vn", UserRole.CUSTOMER, 1L);
            seed("customer02", "customer123", "Trần Thị B", "b@rikkeibank.vn", UserRole.CUSTOMER, 2L);
        };
    }

    private void seed(String username, String rawPassword, String fullName, String email,
                      UserRole role, Long customerId) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .email(email)
                .role(role)
                .customerId(customerId)
                .status(UserStatus.ACTIVE)
                .build());
        log.info("[INIT] Đã tạo tài khoản mẫu username={} role={}", username, role);
    }
}
