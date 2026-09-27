package com.rikkeibank.identity.service;

import com.rikkeibank.common.error.BusinessException;
import com.rikkeibank.common.error.ResourceNotFoundException;
import com.rikkeibank.identity.dto.IdentityDtos.RegisterRequest;
import com.rikkeibank.identity.dto.IdentityDtos.UpdateUserRequest;
import com.rikkeibank.identity.dto.IdentityDtos.UserResponse;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.entity.UserRole;
import com.rikkeibank.identity.entity.UserStatus;
import com.rikkeibank.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Quản lý người dùng (chủ yếu cho ADMIN). */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("USERNAME_EXISTS", "Tên đăng nhập đã tồn tại");
        }
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .customerId(request.customerId())
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .build();
        log.info("[USER] Đăng ký người dùng mới username={} role={}", request.username(), request.role());
        return toResponse(userRepository.save(user));
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = find(id);
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.status() != null) {
            user.setStatus(parseStatus(request.status()));
        }
        log.info("[USER] Cập nhật người dùng id={}", id);
        return toResponse(userRepository.save(user));
    }

    /** Khoá / mở khoá tài khoản - chỉ ADMIN. */
    @Transactional
    public UserResponse changeStatus(Long id, String status) {
        User user = find(id);
        user.setStatus(parseStatus(status));
        log.warn("[USER][SECURITY] Đổi trạng thái user id={} -> {}", id, user.getStatus());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = find(id);
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("CANNOT_DELETE_ADMIN", "Không thể xoá tài khoản ADMIN");
        }
        log.warn("[USER] Xoá người dùng id={} username={}", id, user.getUsername());
        userRepository.delete(user);
    }

    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("INVALID_STATUS", "Trạng thái không hợp lệ: " + status);
        }
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng id=" + id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.getCustomerId(), user.getRole(), user.getStatus().name());
    }
}
