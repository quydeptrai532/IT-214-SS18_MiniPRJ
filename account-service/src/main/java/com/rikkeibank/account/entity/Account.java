package com.rikkeibank.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tài khoản ngân hàng.
 *
 * Trường @Version dùng OPTIMISTIC LOCKING: nếu hai giao dịch cùng sửa số dư một tài khoản,
 * giao dịch đến sau sẽ bị từ chối (OptimisticLockException) -> không bao giờ mất tiền do
 * ghi đè lẫn nhau (lost update).
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String accountNumber;

    /** Chủ tài khoản (id bên customer-service - Database per service nên chỉ lưu id). */
    @Column(nullable = false)
    private Long customerId;

    /** Loại tài khoản (id bên customer-service). */
    @Column(nullable = false)
    private Long accountTypeId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    /** ACTIVE | LOCKED | CLOSED */
    @Column(nullable = false, length = 20)
    private String status;

    @Version
    private Long version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (currency == null) {
            currency = "VND";
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
