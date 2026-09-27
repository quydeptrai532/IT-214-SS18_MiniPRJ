package com.rikkeibank.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CustomerRequest(
            @NotBlank String fullName,
            @NotBlank String identityNumber,
            @NotBlank @Email String email,
            String phone,
            String address,
            LocalDate dateOfBirth) {
    }

    public record CustomerResponse(Long id, String customerCode, String fullName, String identityNumber,
                                   String email, String phone, String address, LocalDate dateOfBirth, String status) {
    }

    public record StaffRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            String phone,
            String branch,
            @NotBlank String position) {
    }

    public record StaffResponse(Long id, String staffCode, String fullName, String email, String phone,
                                String branch, String position, String status) {
    }

    public record AccountTypeRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotNull BigDecimal minimumBalance,
            Boolean active) {
    }

    public record AccountTypeResponse(Long id, String code, String name, String description,
                                      BigDecimal minimumBalance, Boolean active) {
    }
}
