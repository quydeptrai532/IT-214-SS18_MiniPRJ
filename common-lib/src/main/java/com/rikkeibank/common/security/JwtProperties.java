package com.rikkeibank.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Cấu hình JWT - đọc từ Config Server (config-repo/application.yml) để MỌI service dùng chung
 * một secret. Nhờ vậy token do identity-service phát hành được tất cả service xác thực độc lập.
 *
 * @param secret         khóa bí mật ký token (HS256, tối thiểu 32 ký tự)
 * @param issuer         đơn vị phát hành
 * @param accessTokenTtl thời gian sống của access token
 * @param refreshTokenTtl thời gian sống của refresh token (dài để khách không phải đăng nhập lại)
 */
@ConfigurationProperties(prefix = "rikkeibank.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("rikkeibank.jwt.secret phải có tối thiểu 32 ký tự");
        }
    }
}
