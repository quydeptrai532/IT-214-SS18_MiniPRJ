package com.rikkeibank.common.security;

/** Các hằng số dùng chung cho bảo mật. */
public final class SecurityConstants {

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /** Header do Gateway gắn thêm khi forward request hợp lệ xuống service. */
    public static final String HEADER_USER_ID = "X-Auth-UserId";
    public static final String HEADER_USERNAME = "X-Auth-Username";
    public static final String HEADER_ROLES = "X-Auth-Roles";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_TELLER = "TELLER";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    /** Tiền tố khóa Redis lưu các token đã bị thu hồi (ép buộc đăng xuất). */
    public static final String REVOKED_TOKEN_KEY_PREFIX = "rikkeibank:revoked:";

    /** Các đường dẫn công khai (không cần đăng nhập). */
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/identity/auth/login",
            "/api/identity/auth/register",
            "/api/identity/auth/refresh",
            "/actuator/health",
            "/actuator/info"
    };

    private SecurityConstants() {
    }
}
