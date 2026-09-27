package com.rikkeibank.common.error;

/** Lỗi nghiệp vụ chung (vi phạm quy tắc kinh doanh). */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
