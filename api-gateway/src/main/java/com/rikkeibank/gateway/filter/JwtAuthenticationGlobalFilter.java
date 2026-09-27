package com.rikkeibank.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.common.error.ApiError;
import com.rikkeibank.common.security.JwtService;
import com.rikkeibank.common.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Xác thực JWT NGAY TẠI CỔNG VÀO (edge authentication).
 *
 * Vì sao kiểm tra ở đây thay vì chỉ ở từng service?
 *   - Chặn sớm request không hợp lệ: tiết kiệm tài nguyên cho service phía sau.
 *   - Là NƠI DUY NHẤT kiểm tra danh sách token đã bị thu hồi (đọc Redis) -> khi ADMIN
 *     "ép buộc đăng xuất", token cũ bị vô hiệu hoá trên toàn hệ thống ngay lập tức.
 *
 * Endpoint công khai (login/register/refresh) được đi thẳng qua.
 */
@Slf4j
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationGlobalFilter(JwtService jwtService,
                                        ReactiveStringRedisTemplate redisTemplate,
                                        ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(SecurityConstants.HEADER_AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(SecurityConstants.BEARER_PREFIX)) {
            log.warn("[GATEWAY] Từ chối {} - thiếu token", path);
            return unauthorized(exchange, "UNAUTHENTICATED", "Chưa đăng nhập. Vui lòng đăng nhập để tiếp tục.");
        }

        String token = authorization.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (Exception ex) {
            log.warn("[GATEWAY] Từ chối {} - token không hợp lệ: {}", path, ex.getMessage());
            return unauthorized(exchange, "INVALID_TOKEN", "Token không hợp lệ hoặc đã hết hạn.");
        }

        if (!jwtService.isAccessToken(claims)) {
            return unauthorized(exchange, "INVALID_TOKEN", "Cần dùng access token, không phải refresh token.");
        }

        String jti = jwtService.jti(claims);
        String revokedKey = SecurityConstants.REVOKED_TOKEN_KEY_PREFIX + jti;

        // Kiểm tra token đã bị thu hồi chưa (Redis). Redis lỗi -> coi như chưa thu hồi (fail-open),
        // để hệ thống không sập chỉ vì cache; đã ghi log để bảo trì.
        return redisTemplate.opsForValue().get(revokedKey)
                .map(value -> Boolean.TRUE)
                .defaultIfEmpty(Boolean.FALSE)
                .onErrorResume(ex -> {
                    log.error("[GATEWAY] Không đọc được Redis khi kiểm tra thu hồi token: {}", ex.getMessage());
                    return Mono.just(Boolean.FALSE);
                })
                .flatMap(revoked -> {
                    if (Boolean.TRUE.equals(revoked)) {
                        log.warn("[GATEWAY] Từ chối {} - token đã bị thu hồi (jti={})", path, jti);
                        return unauthorized(exchange, "TOKEN_REVOKED",
                                "Phiên đăng nhập đã bị thu hồi. Vui lòng đăng nhập lại.");
                    }
                    return chain.filter(withAuthHeaders(exchange, claims));
                });
    }

    /** Gắn thông tin người dùng vào header để service phía sau dùng cho phân quyền/audit. */
    private ServerWebExchange withAuthHeaders(ServerWebExchange exchange, Claims claims) {
        String roles = String.join(",", jwtService.authorities(claims));
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(SecurityConstants.HEADER_USER_ID, String.valueOf(jwtService.userId(claims)))
                .header(SecurityConstants.HEADER_USERNAME, claims.getSubject())
                .header(SecurityConstants.HEADER_ROLES, roles)
                .build();
        return exchange.mutate().request(request).build();
    }

    private boolean isPublic(String path) {
        return List.of(SecurityConstants.PUBLIC_ENDPOINTS).stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(
                    ApiError.of(code, message, exchange.getRequest().getURI().getPath()));
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception ex) {
            byte[] fallback = ("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
