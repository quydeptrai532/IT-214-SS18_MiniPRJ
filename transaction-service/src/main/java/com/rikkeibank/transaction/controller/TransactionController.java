package com.rikkeibank.transaction.controller;

import com.rikkeibank.common.security.AuthUser;
import com.rikkeibank.transaction.dto.TransactionDtos.TransactionSummary;
import com.rikkeibank.transaction.dto.TransactionDtos.TransferRequest;
import com.rikkeibank.transaction.dto.TransactionDtos.TransferResponse;
import com.rikkeibank.transaction.service.TransferSagaOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransferSagaOrchestrator orchestrator;

    /** Chuyển khoản - CUSTOMER và TELLER đều dùng được. */
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('CUSTOMER','TELLER','ADMIN')")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                     @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.transfer(request, user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TransferResponse getById(@PathVariable Long id) {
        return orchestrator.getById(id);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public TransferResponse getByCode(@PathVariable String code) {
        return orchestrator.getByCode(code);
    }

    /** Giao dịch viên xem danh sách giao dịch đã thực hiện trong ngày. */
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('TELLER','ADMIN')")
    public List<TransactionSummary> getToday() {
        return orchestrator.getTodayTransactions();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<TransactionSummary> getAll() {
        return orchestrator.getAll();
    }

    /** Lịch sử giao dịch của một tài khoản (khách chỉ xem được của mình - kiểm tra ở tầng service/ownership). */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','CUSTOMER')")
    public List<TransactionSummary> getByAccount(@PathVariable Long accountId) {
        return orchestrator.getByAccount(accountId);
    }
}
