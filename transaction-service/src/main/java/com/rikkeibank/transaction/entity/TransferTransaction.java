package com.rikkeibank.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Bản ghi giao dịch chuyển khoản (Transaction) - ghi nhận biến động số dư ghi nợ/ghi có. */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferTransaction {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_COMPENSATED = "COMPENSATED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String transactionCode;

    @Column(nullable = false)
    private Long fromAccountId;

    @Column(nullable = false)
    private Long toAccountId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String failureReason;

    @Column(length = 500)
    private String description;

    /** Khóa chống trùng: cùng một yêu cầu gửi lại sẽ không tạo giao dịch thứ hai. */
    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    private Long createdByUserId;

    @Column(length = 50)
    private String createdByUsername;

    @Column(length = 20)
    private String createdByRole;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (currency == null) {
            currency = "VND";
        }
        if (status == null) {
            status = STATUS_PENDING;
        }
    }
}
