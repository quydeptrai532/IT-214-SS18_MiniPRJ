# RikkeiBank API — Ngân hàng số (Microservice)

Mini Project — hệ thống Backend Microservice cho ngân hàng số RikkeiBank
(Spring Boot 3.3.5 · Spring Cloud 2023.0.3 · Java 17 · MySQL · Kafka · Redis · JWT).

## 1. Kiến trúc

```
                       ┌───────────────────────────────┐
   Client / Mobile ───►│   API GATEWAY  :8080          │
                       │   - Định tuyến (lb://)        │
                       │   - Xác thực JWT tại biên      │
                       │   - Chặn token đã bị thu hồi   │
                       └────────────┬──────────────────┘
                                    │ Eureka :8761 / Config Server :8888
   ┌────────────┬───────────────┬───┴────────────┬──────────────────┐
   ▼            ▼               ▼                ▼                  ▼
identity     customer        account        transaction        notification
 :8081        :8082           :8083            :8084              :8085
 JWT/Users    Khách hàng      Tài khoản        Chuyển khoản       WebFlux +
 Phân quyền   Nhân viên       Số dư            (Saga gốc)         Kafka reactive
              Loại TK         Redis cache      Ghi nợ/Ghi có      Thông báo
   │            │               │                │                  ▲
   │            │  Feign ◄──────┘                │                  │
   │            │  (Circuit Breaker)             │                  │
   └────────────┴───────────────┴─── Kafka ──────┴──────────────────┘
                        rikkeibank.transaction.events
                        rikkeibank.account.events
```

| Service | Port | Vai trò | Database riêng |
|---|---|---|---|
| config-server | 8888 | Cấu hình tập trung (native) | — |
| eureka-server | 8761 | Service Registry | — |
| api-gateway | 8080 | Cổng vào duy nhất + xác thực JWT | — |
| identity-service | 8081 | Đăng nhập, JWT, người dùng, thu hồi phiên | rikkeibank_identity |
| customer-service | 8082 | Khách hàng, nhân viên, loại tài khoản | rikkeibank_customer |
| account-service | 8083 | Tài khoản, số dư, Redis Cache-Aside | rikkeibank_account |
| transaction-service | 8084 | **Orchestrator Saga** chuyển khoản | rikkeibank_transaction |
| notification-service | 8085 | **WebFlux + Kafka reactive** | (in-memory) |

## 2. Công nghệ đã áp dụng (theo SRS)

| Yêu cầu | Triển khai |
|---|---|
| RESTful + JSON + HTTP status chuẩn | `@RestController`, lỗi trả `400/401/403/404/422/500` |
| Spring Cloud Config | `config-repo/` (native), mọi service `spring.config.import` |
| Eureka | `@EnableEurekaServer` + `@EnableDiscoveryClient` ở mọi service |
| Gateway + Load Balancing | 5 route `lb://`, `spring-cloud-starter-loadbalancer` |
| Đồng bộ service-to-service | OpenFeign: transaction→account, account→customer |
| Bất đồng bộ event-driven | Kafka + `reactor-kafka` (`KafkaReceiver` → `Flux`) ở notification-service |
| Fault Tolerance | Resilience4j CircuitBreaker + fallbackFactory (CLOSED/OPEN/HALF-OPEN) |
| Distributed Caching | Spring Cache + Redis, `@Cacheable`/`@CachePut`/`@CacheEvict` |
| Saga Pattern | Orchestrator (transaction-service) + COMPENSATING khi ghi có thất bại |
| Database-per-service | 4 DB độc lập, không service nào truy cập DB của service khác |
| JWT + phân quyền | identity-service cấp token; `@PreAuthorize` theo ADMIN/TELLER/CUSTOMER |
| Thu hồi quyền truy cập | Redis: `revoked:<jti>` và `tokenNotBefore:<userId>` (ép đăng xuất mọi thiết bị) |
| Exception Handling + AOP | `GlobalExceptionHandler` (common-lib) + `ServiceLoggingAspect` |
| Unit Test + Jacoco | `jacoco-maven-plugin` cấu hình ở parent pom; xem mục 5 |

## 3. Chạy hệ thống

```bash
# 1. Hạ tầng: MySQL + Kafka (KRaft) + Redis
docker compose up -d

# 2. Build
mvn clean install

# 3. Chạy theo ĐÚNG THỨ TỰ (Config Server trước, rồi Eureka)
mvn -pl config-server     spring-boot:run    # :8888
mvn -pl eureka-server     spring-boot:run    # :8761
mvn -pl identity-service  spring-boot:run    # :8081
mvn -pl customer-service  spring-boot:run    # :8082
mvn -pl account-service   spring-boot:run    # :8083
mvn -pl transaction-service spring-boot:run  # :8084
mvn -pl notification-service spring-boot:run # :8085
mvn -pl api-gateway       spring-boot:run    # :8080
```

**Lưu ý MySQL:** mặc định `config-repo/*.yml` dùng `root/123456` (khớp docker-compose).
Nếu dùng MySQL cài sẵn trên máy với mật khẩu khác, truyền `DB_PASSWORD`:

```bash
# Windows PowerShell
$env:DB_PASSWORD='12345678'; mvn -pl account-service spring-boot:run
```

## 4. Tài khoản mẫu (tự tạo khi khởi động identity-service)

| Username | Password | Vai trò |
|---|---|---|
| admin | admin123 | ADMIN |
| teller01 | teller123 | TELLER |
| customer01 | customer123 | CUSTOMER |
| customer02 | customer123 | CUSTOMER |

## 5. Test & độ bao phủ

```bash
mvn test          # chạy toàn bộ unit test + sinh báo cáo Jacoco
```

Báo cáo độ bao phủ: `target/site/jacoco/index.html` của từng module.

## 6. Postman

Import `postman/RikkeiBank.postman_collection.json` — collection được chia thư mục theo từng service,
tất cả request đều đi qua Gateway (`http://localhost:8080`). Biến `accessToken` được tự động lưu
sau khi chạy request đăng nhập.

## 7. Ghi chú thiết kế

- **Vì sao Orchestrator Saga?** Nghiệp vụ chuyển khoản cần biết rõ đang ở bước nào để bù trừ đúng
  (ghi nợ xong nhưng ghi có lỗi → phải hoàn tiền). Nhật ký `saga_steps` lưu từng bước phục vụ điều tra.
- **Không dùng `@Transactional` cho toàn bộ Saga**: mỗi bước là transaction riêng; nếu bọc một
  transaction lớn thì khi lỗi, nhật ký Saga cũng bị rollback → mất dấu vết.
- **Chống trùng**: `idempotencyKey` (chuyển khoản), `jti` (token), `@Version` (số dư - optimistic lock).
- **Đăng xuất lâu dài**: refresh token 7 ngày, access token 30 phút → khách không phải đăng nhập lại liên tục.
- **Hạn chế đã biết**: Kafka phải chạy qua docker-compose; nếu chấm offline, chạy `mvn test`
  vẫn được vì unit test dùng mock/H2, không cần broker.
