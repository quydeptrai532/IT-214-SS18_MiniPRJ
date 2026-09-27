package com.rikkeibank.identity.dto;

import com.rikkeibank.identity.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Gom các DTO của identity-service vào một chỗ cho gọn. */
public final class IdentityDtos {

    private IdentityDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "username không được để trống") String username,
            @NotBlank(message = "password không được để trống") String password) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 4, max = 50) String username,
            @NotBlank @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự") String password,
            @NotBlank String fullName,
            @Email String email,
            String phone,
            Long customerId,
            @NotNull UserRole role) {
    }

    public record RefreshTokenRequest(
            @NotBlank(message = "refreshToken không được để trống") String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType,
                                long expiresInSeconds, String username, String fullName, String role) {
    }

    public record UserResponse(Long id, String username, String fullName, String email, String phone,
                               Long customerId, UserRole role, String status) {
    }

    public record UpdateUserRequest(String fullName, String email, String phone,
                                    UserRole role, String status) {
    }

    public record ChangePasswordRequest(@NotBlank String oldPassword,
                                        @NotBlank @Size(min = 6) String newPassword) {
    }

    public record MessageResponse(String code, String message) {
    }
}
