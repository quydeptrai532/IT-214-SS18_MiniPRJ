package com.rikkeibank.account.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Gọi customer-service theo TÊN service (LoadBalancer + Eureka).
 * Kiểm tra chủ tài khoản có tồn tại trước khi mở tài khoản mới.
 */
@FeignClient(name = "customer-service", path = "/api/customers",
        fallbackFactory = CustomerClientFallbackFactory.class)
public interface CustomerClient {

    @GetMapping("/{id}")
    Object getCustomer(@PathVariable("id") Long id);
}
