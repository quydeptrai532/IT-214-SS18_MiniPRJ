package com.rikkeibank.identity.controller;

import com.rikkeibank.identity.dto.IdentityDtos.MessageResponse;
import com.rikkeibank.identity.dto.IdentityDtos.RegisterRequest;
import com.rikkeibank.identity.dto.IdentityDtos.UpdateUserRequest;
import com.rikkeibank.identity.dto.IdentityDtos.UserResponse;
import com.rikkeibank.identity.service.AuthService;
import com.rikkeibank.identity.service.UserService;
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
import java.util.Map;

/**
 * Quản trị người dùng - yêu cầu vai trò ADMIN (trừ endpoint /me).
 * Đặc biệt: POST /{id}/force-logout để thu hồi quyền truy cập ngay lập tức.
 */
@RestController
@RequestMapping("/api/identity/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAll();
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    /** ADMIN tạo tài khoản với bất kỳ vai trò nào (ADMIN / TELLER / CUSTOMER). */
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public UserResponse changeStatus(@PathVariable Long id, @RequestParam String status) {
        return userService.changeStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * THU HỒI QUYỀN TRUY CẬP: ADMIN ép một tài khoản đăng xuất khỏi mọi thiết bị ngay lập tức.
     * Mọi token phát hành trước thời điểm này sẽ bị Gateway từ chối (401).
     */
    @PostMapping("/{id}/force-logout")
    public ResponseEntity<Map<String, Object>> forceLogout(@PathVariable Long id) {
        authService.forceLogout(id);
        return ResponseEntity.ok(Map.of(
                "code", "FORCE_LOGOUT_SUCCESS",
                "message", "Đã thu hồi toàn bộ phiên đăng nhập của người dùng id=" + id));
    }
}
