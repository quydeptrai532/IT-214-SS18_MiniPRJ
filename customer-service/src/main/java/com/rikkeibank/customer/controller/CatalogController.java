package com.rikkeibank.customer.controller;

import com.rikkeibank.customer.dto.CustomerDtos.AccountTypeRequest;
import com.rikkeibank.customer.dto.CustomerDtos.AccountTypeResponse;
import com.rikkeibank.customer.dto.CustomerDtos.StaffRequest;
import com.rikkeibank.customer.dto.CustomerDtos.StaffResponse;
import com.rikkeibank.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Quản lý nhân viên và loại tài khoản - chỉ ADMIN. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CatalogController {

    private final CustomerService customerService;

    @GetMapping("/staffs")
    public List<StaffResponse> getAllStaffs() {
        return customerService.getAllStaffs();
    }

    @GetMapping("/staffs/{id}")
    public StaffResponse getStaff(@PathVariable Long id) {
        return customerService.getStaffById(id);
    }

    @PostMapping("/staffs")
    public ResponseEntity<StaffResponse> createStaff(@Valid @RequestBody StaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createStaff(request));
    }

    @PutMapping("/staffs/{id}")
    public StaffResponse updateStaff(@PathVariable Long id, @Valid @RequestBody StaffRequest request) {
        return customerService.updateStaff(id, request);
    }

    @DeleteMapping("/staffs/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        customerService.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/account-types")
    public List<AccountTypeResponse> getAllAccountTypes() {
        return customerService.getAllAccountTypes();
    }

    @GetMapping("/account-types/{id}")
    public AccountTypeResponse getAccountType(@PathVariable Long id) {
        return customerService.getAccountType(id);
    }

    @PostMapping("/account-types")
    public ResponseEntity<AccountTypeResponse> createAccountType(
            @Valid @RequestBody AccountTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createAccountType(request));
    }

    @PutMapping("/account-types/{id}")
    public AccountTypeResponse updateAccountType(@PathVariable Long id,
                                                 @Valid @RequestBody AccountTypeRequest request) {
        return customerService.updateAccountType(id, request);
    }

    @DeleteMapping("/account-types/{id}")
    public ResponseEntity<Void> deleteAccountType(@PathVariable Long id) {
        customerService.deleteAccountType(id);
        return ResponseEntity.noContent().build();
    }
}
