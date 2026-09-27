package com.rikkeibank.identity.entity;

public enum UserStatus {
    ACTIVE,
    /** Bị khoá: không thể đăng nhập (do ADMIN khoá hoặc phát hiện rủi ro). */
    LOCKED
}
