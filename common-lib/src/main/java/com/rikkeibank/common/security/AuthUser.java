package com.rikkeibank.common.security;

/** Thông tin người dùng đã xác thực - được đặt làm principal trong SecurityContext. */
public record AuthUser(Long userId, String username, String role, String fullName) {

    public boolean isAdmin() {
        return SecurityConstants.ROLE_ADMIN.equals(role);
    }

    public boolean isTeller() {
        return SecurityConstants.ROLE_TELLER.equals(role);
    }

    public boolean isCustomer() {
        return SecurityConstants.ROLE_CUSTOMER.equals(role);
    }
}
