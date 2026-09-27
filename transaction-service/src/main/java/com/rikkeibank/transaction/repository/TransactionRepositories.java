package com.rikkeibank.transaction.repository;

import com.rikkeibank.transaction.entity.SagaStep;
import com.rikkeibank.transaction.entity.TransferTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class TransactionRepositories {

    private TransactionRepositories() {
    }

    @Repository
    public interface TransactionRepository extends JpaRepository<TransferTransaction, Long> {
        Optional<TransferTransaction> findByTransactionCode(String transactionCode);

        Optional<TransferTransaction> findByIdempotencyKey(String idempotencyKey);

        List<TransferTransaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

        List<TransferTransaction> findByFromAccountIdOrToAccountId(Long fromAccountId, Long toAccountId);
    }

    @Repository
    public interface SagaStepRepository extends JpaRepository<SagaStep, Long> {
        List<SagaStep> findByTransactionIdOrderByIdAsc(Long transactionId);
    }
}
