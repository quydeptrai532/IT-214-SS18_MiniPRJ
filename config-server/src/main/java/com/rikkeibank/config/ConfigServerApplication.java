package com.rikkeibank.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * RikkeiBank - Config Server (Cấu hình tập trung).
 *
 * Chạy profile "native": đọc trực tiếp thư mục config-repo của project (không cần git server).
 * Kiểm tra: http://localhost:8888/account-service/default
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
