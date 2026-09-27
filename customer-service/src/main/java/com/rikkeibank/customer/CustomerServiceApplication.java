package com.rikkeibank.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/** RikkeiBank - Customer Service: quản lý Khách hàng, Nhân viên, Loại tài khoản. */
@SpringBootApplication(scanBasePackages = {"com.rikkeibank.customer", "com.rikkeibank.common"})
@EnableDiscoveryClient
@EnableCaching
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
