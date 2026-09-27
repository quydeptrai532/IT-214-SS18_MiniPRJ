package com.rikkeibank.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * RikkeiBank - Identity Service.
 * Chịu trách nhiệm: đăng nhập, cấp JWT (access + refresh), quản lý người dùng, phân quyền,
 * và thu hồi phiên (ép buộc đăng xuất) khi ADMIN phát hiện rủi ro bảo mật.
 */
@SpringBootApplication(scanBasePackages = {"com.rikkeibank.identity", "com.rikkeibank.common"})
@EnableDiscoveryClient
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
