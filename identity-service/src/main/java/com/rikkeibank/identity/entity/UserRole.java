package com.rikkeibank.identity.entity;

public enum UserRole {
    /** Quản trị viên: toàn quyền, có thể ép một tài khoản đăng xuất. */
    ADMIN,
    /** Giao dịch viên: xử lý/xem giao dịch trong phạm vi được phân công. */
    TELLER,
    /** Khách hàng: chỉ xem tài khoản & giao dịch của chính mình. */
    CUSTOMER
}
