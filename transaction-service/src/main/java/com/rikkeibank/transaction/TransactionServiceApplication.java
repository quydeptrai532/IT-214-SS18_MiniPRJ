package com.rikkeibank.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RikkeiBank - Transaction Service.
 * Đóng vai ORCHESTRATOR của Saga chuyển khoản: điều khiển tuần tự các bước
 * (ghi nợ tài khoản nguồn -> ghi có tài khoản đích) và chạy COMPENSATING
 * (hoàn tiền) nếu một bước thất bại.
 */
@SpringBootApplication(scanBasePackages = {"com.rikkeibank.transaction", "com.rikkeibank.common"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
