package com.rikkeibank.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Sinh và xác thực JWT.
 *
 * Token chứa: userId, username, role, fullName, jti (id của token - dùng để thu hồi khi ADMIN
 * ép buộc đăng xuất), typ (access | refresh).
 *
 * Dùng chung cho: identity-service (phát hành), api-gateway (kiểm tra ở biên),
 * và tất cả service nghiệp vụ (tự xác thực để không phụ thuộc hoàn toàn vào gateway).
 */
@Service
public class JwtService {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_FULL_NAME = "fullName";
    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username, String role, String fullName) {
        return buildToken(userId, username, role, fullName, TYPE_ACCESS, properties.accessTokenTtl());
    }

    public String generateRefreshToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, null, TYPE_REFRESH, properties.refreshTokenTtl());
    }

    private String buildToken(Long userId, String username, String role, String fullName,
                              String type, java.time.Duration ttl) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .issuer(properties.issuer())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_FULL_NAME, fullName)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(signingKey)
                .compact();
    }

    /** Xác thực chữ ký + hạn dùng, trả về claims. Ném JwtException nếu token không hợp lệ. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public Long userId(Claims claims) {
        Number value = claims.get(CLAIM_USER_ID, Number.class);
        return value == null ? null : value.longValue();
    }

    public String role(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    public String jti(Claims claims) {
        return claims.getId();
    }

    public List<String> authorities(Claims claims) {
        String role = role(claims);
        return role == null ? List.of() : List.of("ROLE_" + role);
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    public long refreshTokenTtlSeconds() {
        return properties.refreshTokenTtl().toSeconds();
    }
}
