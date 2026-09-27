package com.rikkeibank.transaction.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback của AccountClient.
 *
 * ĐIỂM QUAN TRỌNG: fallback NÉM ServiceUnavailableException thay vì trả null.
 * Nhờ vậy tầng Saga bắt được và coi đó là "bước thất bại" -> kích hoạt compensating.
 * (Nếu fallback âm thầm trả null thì Saga tưởng thành công -> dữ liệu du lệch.)
 */
@Slf4j
@Component
public class AccountClientFallbackFactory implements FallbackFactory<AccountClient> {

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) {
            super(message);
        }
    }

    @Override
    public AccountClient create(Throwable cause) {
        return new AccountClient() {
            @Override
            public BalanceResponse debit(Long accountId, BalanceChangeRequest request, Long transactionId) {
                log.error("[FALLBACK] Không ghi nợ được tài khoản {}: {}", accountId, cause.toString());
                throw new ServiceUnavailableException("account-service không khả dụng khi ghi nợ tài khoản " + accountId);
            }

            @Override
            public BalanceResponse credit(Long accountId, BalanceChangeRequest request, Long transactionId) {
                log.error("[FALLBACK] Không ghi có được tài khoản {}: {}", accountId, cause.toString());
                throw new ServiceUnavailableException("account-service không khả dụng khi ghi có tài khoản " + accountId);
            }
        };
    }
}
