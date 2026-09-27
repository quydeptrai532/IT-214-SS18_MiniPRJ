package com.rikkeibank.transaction.client;

import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * Gọi account-service theo TÊN service (Eureka + LoadBalancer).
 * Đây là các bước của Saga: ghi nợ tài khoản nguồn, ghi có tài khoản đích,
 * và ghi có lại tài khoản nguồn khi cần BÙ TRỪ.
 */
@FeignClient(name = "account-service", path = "/api/accounts",
        fallbackFactory = AccountClientFallbackFactory.class, configuration = AccountClientConfig.class)
public interface AccountClient {

    record BalanceChangeRequest(@NotNull BigDecimal amount, String description) {
    }

    record BalanceResponse(Long accountId, String accountNumber, BigDecimal balance, String currency) {
    }

    @PostMapping("/{id}/debit")
    BalanceResponse debit(@PathVariable("id") Long accountId,
                          @RequestBody BalanceChangeRequest request,
                          @RequestParam(value = "transactionId", required = false) Long transactionId);

    @PostMapping("/{id}/credit")
    BalanceResponse credit(@PathVariable("id") Long accountId,
                           @RequestBody BalanceChangeRequest request,
                           @RequestParam(value = "transactionId", required = false) Long transactionId);
}
