package com.rikkeibank.account.controller;

import com.rikkeibank.account.dto.AccountDtos.AccountResponse;
import com.rikkeibank.account.dto.AccountDtos.BalanceChangeRequest;
import com.rikkeibank.account.dto.AccountDtos.BalanceResponse;
import com.rikkeibank.account.dto.AccountDtos.CreateAccountRequest;
import com.rikkeibank.account.dto.AccountDtos.UpdateAccountRequest;
import com.rikkeibank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public List<AccountResponse> getAll() {
        return accountService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AccountResponse getById(@PathVariable Long id) {
        return accountService.getAccountById(id);
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("isAuthenticated()")
    public BalanceResponse getBalance(@PathVariable Long id) {
        return accountService.getBalance(id);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER') or @accountOwnershipChecker.isOwner(authentication, #customerId)")
    public List<AccountResponse> getByCustomer(@PathVariable Long customerId) {
        return accountService.getAccountsByCustomer(customerId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse update(@PathVariable Long id, @RequestBody UpdateAccountRequest request) {
        return accountService.updateAccount(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse changeStatus(@PathVariable Long id, @RequestParam String status) {
        return accountService.changeStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    // ===== Hai endpoint dưới đây do transaction-service gọi nội bộ (Saga) =====

    @PostMapping("/{id}/debit")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER') or hasRole('CUSTOMER')")
    public BalanceResponse debit(@PathVariable Long id, @Valid @RequestBody BalanceChangeRequest request,
                                 @RequestParam(required = false) Long transactionId) {
        return accountService.debit(id, request.amount(), transactionId);
    }

    @PostMapping("/{id}/credit")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER') or hasRole('CUSTOMER')")
    public BalanceResponse credit(@PathVariable Long id, @Valid @RequestBody BalanceChangeRequest request,
                                  @RequestParam(required = false) Long transactionId) {
        return accountService.credit(id, request.amount(), transactionId);
    }
}
