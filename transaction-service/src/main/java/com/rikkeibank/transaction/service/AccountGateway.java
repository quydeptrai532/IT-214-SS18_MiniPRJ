package com.rikkeibank.transaction.service;

import com.rikkeibank.transaction.client.AccountClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Cổng duy nhất gọi account-service.
 *
 * Cơ chế kháng lỗi nằm ở tầng Feign (Spring Cloud OpenFeign + Resilience4j Circuit Breaker):
 *   spring.cloud.openfeign.circuitbreaker.enabled = true
 *   spring.cloud.openfeign.circuitbreaker.group.enabled = true   -> mạch dùng chung theo tên client
 *   AccountClient#fallbackFactory = AccountClientFallbackFactory   -> xử lý khi mạch mở / gọi lỗi
 *
 * Nhờ vậy khi account-service chết, lời gọi bị chặn và trả lỗi rõ ràng thay vì treo hàng loạt.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountGateway {

    private final AccountClient accountClient;

    public AccountClient.BalanceResponse debit(Long accountId, BigDecimal amount, Long transactionId) {
        log.info("[SAGA] Bước GHI NỢ: tài khoản {} số tiền {}", accountId, amount);
        return accountClient.debit(accountId,
                new AccountClient.BalanceChangeRequest(amount, "Chuyển khoản - ghi nợ"), transactionId);
    }

    public AccountClient.BalanceResponse credit(Long accountId, BigDecimal amount, Long transactionId) {
        log.info("[SAGA] Bước GHI CÓ: tài khoản {} số tiền {}", accountId, amount);
        return accountClient.credit(accountId,
                new AccountClient.BalanceChangeRequest(amount, "Chuyển khoản - ghi có"), transactionId);
    }
}
