package com.rikkeibank.common.event;

/** Loại sự kiện nghiệp vụ ngân hàng. */
public enum BankEventType {

    /** Bắt đầu chuyển khoản (đã trừ tiền tài khoản nguồn). */
    TRANSFER_DEBITED,
    /** Chuyển khoản hoàn tất (đã cộng tiền tài khoản đích). */
    TRANSFER_COMPLETED,
    /** Chuyển khoản thất bại. */
    TRANSFER_FAILED,
    /** Đã hoàn tác (compensating) - trả lại tiền cho tài khoản nguồn. */
    TRANSFER_COMPENSATED,
    /** Số dư tài khoản thay đổi. */
    ACCOUNT_BALANCE_CHANGED,
    /** Tài khoản bị khoá/mở khoá bởi ADMIN. */
    ACCOUNT_STATUS_CHANGED,
    /** Tài khoản người dùng bị ép buộc đăng xuất. */
    USER_FORCE_LOGOUT
}
