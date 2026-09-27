package com.rikkeibank.common.error;

import java.time.Instant;
import java.util.Map;

/** Cấu trúc lỗi CHUẨN cho toàn hệ thống (mọi service trả về cùng một định dạng). */
public record ApiError(String code, String message, Map<String, String> details, String path, Instant timestamp) {

    public static ApiError of(String code, String message, String path) {
        return new ApiError(code, message, Map.of(), path, Instant.now());
    }

    public static ApiError of(String code, String message, Map<String, String> details, String path) {
        return new ApiError(code, message, details, path, Instant.now());
    }
}
