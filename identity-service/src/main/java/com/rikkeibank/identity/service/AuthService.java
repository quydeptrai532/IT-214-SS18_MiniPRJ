package com.rikkeibank.identity.service;

import com.rikkeibank.common.error.BusinessException;
import com.rikkeibank.common.error.ResourceNotFoundException;
import com.rikkeibank.common.security.JwtService;
import com.rikkeibank.common.security.SecurityConstants;
import com.rikkeibank.identity.dto.IdentityDtos.LoginRequest;
import com.rikkeibank.identity.dto.IdentityDtos.RegisterRequest;
import com.rikkeibank.identity.dto.IdentityDtos.TokenResponse;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.entity.UserStatus;
import com.rikkeibank.identity.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;

/**
 * Nghiệp vụ xác thực: đăng nhập, làm mới token, đăng xuất, và THU HỒI PHIÊN.
 *
 * Cơ chế thu hồi (ép buộc đăng xuất):
 *   - Đăng xuất 1 phiên  : ghi jti vào Redis  key = rikkeibank:revoked:<jti>
 *   - Ép buộc đăng xuất   : ghi mốc thời gian key = rikkeibank:tokenNotBefore:<userId>
 *     => mọi token phát hành TRƯỚC mốc này đều bị coi là không hợp lệ (áp dụng cho TẤT CẢ thiết bị).
 *   Gateway đọc 2 khóa này để chặn ngay tại cửa vào.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String TOKEN_NOT_BEFORE_PREFIX = "rikkeibank:tokenNotBefore:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS",
                        "Tên đăng nhập hoặc mật khẩu không đúng"));

        if (user.getStatus() == UserStatus.LOCKED) {
            log.warn("[AUTH] Tài khoản {} đang bị khoá, từ chối đăng nhập", user.getUsername());
            throw new BusinessException("ACCOUNT_LOCKED", "Tài khoản đã bị khoá. Liên hệ quản trị viên.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng");
        }

        // Xoá mốc thu hồi cũ để phiên mới hoạt động bình thường
        redisTemplate.delete(TOKEN_NOT_BEFORE_PREFIX + user.getId());

        log.info("[AUTH] Đăng nhập thành công user={} role={}", user.getUsername(), user.getRole());
        return buildTokens(user);
    }

    public TokenResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (Exception ex) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token không hợp lệ hoặc đã hết hạn");
        }
        if (JwtService.TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
            User user = userRepository.findByUsername(claims.getSubject())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
            if (user.getStatus() == UserStatus.LOCKED) {
                throw new BusinessException("ACCOUNT_LOCKED", "Tài khoản đã bị khoá.");
            }
            log.info("[AUTH] Làm mới token cho user={}", user.getUsername());
            return buildTokens(user);
        }
        throw new BusinessException("INVALID_REFRESH_TOKEN", "Token cung cấp không phải refresh token");
    }

    /** Đăng xuất phiên hiện tại: đưa jti của access token vào danh sách thu hồi. */
    public void logout(String accessToken) {
        try {
            Claims claims = jwtService.parse(accessToken);
            String jti = jwtService.jti(claims);
            long ttlSeconds = Math.max(1, (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000);
            redisTemplate.opsForValue().set(SecurityConstants.REVOKED_TOKEN_KEY_PREFIX + jti,
                    "logout", Duration.ofSeconds(ttlSeconds));
            log.info("[AUTH] Đã thu hồi token jti={} của user={}", jti, claims.getSubject());
        } catch (Exception ex) {
            throw new BusinessException("INVALID_TOKEN", "Token không hợp lệ");
        }
    }

    /**
     * ADMIN ép buộc một tài khoản đăng xuất khỏi MỌI thiết bị ngay lập tức.
     * Mọi token phát hành trước thời điểm này sẽ bị Gateway từ chối.
     */
    @Transactional
    public void forceLogout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng id=" + userId));

        long now = new Date().getTime();
        // Lưu với TTL bằng thời gian sống tối đa của refresh token (sau đó không cần lưu nữa)
        redisTemplate.opsForValue().set(TOKEN_NOT_BEFORE_PREFIX + userId, String.valueOf(now),
                Duration.ofSeconds(jwtService.refreshTokenTtlSeconds()));

        log.warn("[AUTH][SECURITY] ADMIN ép buộc đăng xuất user id={} ({}) - mọi token cũ đã bị vô hiệu hoá",
                userId, user.getUsername());
    }

    private TokenResponse buildTokens(User user) {
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getUsername(), user.getRole().name(), user.getFullName());
        String refreshToken = jwtService.generateRefreshToken(
                user.getId(), user.getUsername(), user.getRole().name());
        return new TokenResponse(accessToken, refreshToken, "Bearer",
                jwtService.accessTokenTtlSeconds(), user.getUsername(), user.getFullName(), user.getRole().name());
    }
}
