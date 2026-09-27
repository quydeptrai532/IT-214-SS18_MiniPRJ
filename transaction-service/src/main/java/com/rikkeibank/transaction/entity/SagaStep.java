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

import java.time.LocalDateTime;

/**
 * Nhật ký từng bước của Saga - phục vụ điều tra khi có sự cố và chứng minh đã chạy bù trừ.
 * Mỗi giao dịch sẽ có các bước: DEBIT, CREDIT, COMPENSATE_DEBIT...
 */
@Entity
@Table(name = "saga_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transactionId;

    @Column(nullable = false, length = 40)
    private String stepName;

    /** SUCCESS | FAILED | COMPENSATED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String message;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
