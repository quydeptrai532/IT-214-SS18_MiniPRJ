package com.rikkeibank.identity.controller;

import com.rikkeibank.common.security.SecurityConstants;
import com.rikkeibank.identity.dto.IdentityDtos.LoginRequest;
import com.rikkeibank.identity.dto.IdentityDtos.MessageResponse;
import com.rikkeibank.identity.dto.IdentityDtos.RefreshTokenRequest;
import com.rikkeibank.identity.dto.IdentityDtos.RegisterRequest;
import com.rikkeibank.identity.dto.IdentityDtos.TokenResponse;
import com.rikkeibank.identity.dto.IdentityDtos.UserResponse;
import com.rikkeibank.identity.entity.UserRole;
import com.rikkeibank.identity.service.AuthService;
import com.rikkeibank.identity.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /** Đăng nhập: trả về access token (ngắn hạn) + refresh token (dài hạn). */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Đăng ký công khai - chỉ cho phép tạo tài khoản CUSTOMER.
     * Tài khoản ADMIN/TELLER phải do ADMIN tạo qua /api/identity/users.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (request.role() != UserRole.CUSTOMER) {
            throw new com.rikkeibank.common.error.BusinessException("FORBIDDEN_ROLE",
                    "Đăng ký công khai chỉ tạo được tài khoản CUSTOMER");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    /** Làm mới access token - giúp khách không phải đăng nhập lại liên tục. */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.HEADER_AUTHORIZATION);
        if (header == null || !header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            throw new com.rikkeibank.common.error.BusinessException("INVALID_TOKEN", "Thiếu access token");
        }
        authService.logout(header.substring(SecurityConstants.BEARER_PREFIX.length()).trim());
        return ResponseEntity.ok(new MessageResponse("LOGGED_OUT", "Đã đăng xuất phiên hiện tại"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> me(org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(authentication.getPrincipal());
    }
}
