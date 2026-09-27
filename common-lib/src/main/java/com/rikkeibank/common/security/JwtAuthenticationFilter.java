package com.rikkeibank.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Đọc Bearer token trong header Authorization, xác thực và đưa thông tin người dùng
 * vào SecurityContext để @PreAuthorize hoạt động.
 *
 * Token sai/hết hạn -> KHÔNG set Authentication -> Spring Security trả 401 qua
 * entry point đã cấu hình trong CommonSecurityConfig.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(SecurityConstants.HEADER_AUTHORIZATION);
        if (header != null && header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String token = header.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
            try {
                Claims claims = jwtService.parse(token);
                if (jwtService.isAccessToken(claims)
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    AuthUser user = new AuthUser(
                            jwtService.userId(claims),
                            claims.getSubject(),
                            jwtService.role(claims),
                            claims.get(JwtService.CLAIM_FULL_NAME, String.class));

                    List<SimpleGrantedAuthority> authorities = jwtService.authorities(claims).stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("[SECURITY] Xác thực thành công user={} role={} uri={}",
                            user.username(), user.role(), request.getRequestURI());
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.warn("[SECURITY] Token không hợp lệ cho {}: {}", request.getRequestURI(), ex.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}
