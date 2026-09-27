package com.rikkeibank.account.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Fallback: customer-service không khả dụng -> trả null để tầng trên báo lỗi rõ ràng. */
@Slf4j
@Component
public class CustomerClientFallbackFactory implements FallbackFactory<CustomerClient> {

    @Override
    public CustomerClient create(Throwable cause) {
        return id -> {
            log.error("[FALLBACK] Không gọi được customer-service để lấy khách hàng id={}. Nguyên nhân: {}",
                    id, cause.toString());
            return null;
        };
    }
}
