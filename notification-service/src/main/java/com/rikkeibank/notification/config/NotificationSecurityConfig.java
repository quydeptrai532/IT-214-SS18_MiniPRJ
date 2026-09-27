package com.rikkeibank.notification.config;

import com.rikkeibank.common.security.JwtProperties;
import com.rikkeibank.common.security.SecurityConstants;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Bảo mật cho ứng dụng WebFlux (notification-service).
 *
 * Vì đây là reactive stack nên KHÔNG dùng được CommonSecurityConfig kiểu servlet của common-lib;
 * module này tự cấu hình SecurityWebFilterChain nhưng vẫn tái sử dụng JwtService để xác thực token.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class NotificationSecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated())
                .build();
    }
}
