package com.rikkeibank.gateway;

import com.rikkeibank.common.security.JwtProperties;
import com.rikkeibank.common.security.JwtService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

/**
 * RikkeiBank - API Gateway.
 *
 * Là cửa vào DUY NHẤT của hệ thống:
 *   - Định tuyến request tới đúng service theo tên đăng ký trên Eureka (uri lb://)
 *   - Cân bằng tải giữa các instance (Spring Cloud LoadBalancer)
 *   - Kiểm tra JWT ngay tại biên + chặn token đã bị thu hồi (đọc Redis)
 *   - Gắn header X-Auth-* cho service phía sau
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(JwtProperties.class)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    /** Dùng chung JwtService từ common-lib nhưng KHÔNG scan toàn bộ package common
     *  (tránh kéo cấu hình bảo mật kiểu servlet vào ứng dụng WebFlux này). */
    @Bean
    public JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }
}
