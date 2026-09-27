package com.rikkeibank.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.common.error.ApiError;
import com.rikkeibank.common.security.JwtAuthenticationFilter;
import com.rikkeibank.common.security.JwtProperties;
import com.rikkeibank.common.security.JwtService;
import com.rikkeibank.common.security.SecurityConstants;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình bảo mật dùng chung cho mọi service (servlet).
 *
 * Nguyên tắc:
 *   - STATELESS: không dùng session phía server, mọi thứ dựa trên JWT.
 *     (Trải nghiệm "không phải đăng nhập lại" được giải quyết bằng refresh token sống lâu
 *      ở phía client, chứ không phải bằng session server.)
 *   - Endpoint công khai: login / register / refresh / actuator health.
 *   - Mọi endpoint còn lại BẮT BUỘC có JWT hợp lệ -> khách vãng lai không xem được dữ liệu tài chính.
 *   - Sai/thiếu token -> 401; đủ token nhưng sai quyền -> 403 (đúng chuẩn HTTP).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class CommonSecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHENTICATED",
                                        "Chưa đăng nhập hoặc token không hợp lệ. Vui lòng đăng nhập lại.",
                                        request.getRequestURI()))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                                        "ACCESS_DENIED",
                                        "Bạn không có quyền thực hiện thao tác này.",
                                        request.getRequestURI())));
        return http.build();
    }

    private void writeError(HttpServletResponse response, ObjectMapper objectMapper, int status,
                            String code, String message, String path) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiError.of(code, message, path));
    }
}
