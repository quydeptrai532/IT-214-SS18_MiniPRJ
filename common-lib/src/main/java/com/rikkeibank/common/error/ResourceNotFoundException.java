package com.rikkeibank.common.error;

/** Không tìm thấy tài nguyên -> HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
