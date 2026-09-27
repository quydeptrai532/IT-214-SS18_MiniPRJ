package com.rikkeibank.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP aspect áp dụng cho TẦNG SERVICE của mọi service trong hệ thống:
 *   - Đo thời gian thực thi (phát hiện truy vấn chậm).
 *   - Ghi log tham số đầu vào / kết quả ở mức DEBUG.
 *   - Ghi log lỗi tập trung ra logger riêng "ERROR_AUDIT" -> phục vụ lịch sử lỗi & bảo trì.
 */
@Slf4j
@Aspect
@Component
public class ServiceLoggingAspect {

    /** Logger riêng cho lịch sử lỗi - có thể cấu hình appender đẩy sang file/ELK. */
    private static final org.slf4j.Logger ERROR_AUDIT = org.slf4j.LoggerFactory.getLogger("ERROR_AUDIT");

    @Pointcut("execution(* com.rikkeibank..service..*(..))")
    public void serviceLayer() {
    }

    @Around("serviceLayer()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("[AOP] --> {} args={}", method, Arrays.toString(joinPoint.getArgs()));
        }
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("[AOP] <-- {} hoàn thành trong {} ms", method, duration);
            if (duration > 2000) {
                log.warn("[AOP] Phương thức {} chạy chậm ({} ms)", method, duration);
            }
            return result;
        } catch (Throwable throwable) {
            log.warn("[AOP] !! {} ném ngoại lệ {} sau {} ms", method,
                    throwable.getClass().getSimpleName(), System.currentTimeMillis() - start);
            throw throwable;
        }
    }

    /** Ghi vết mọi ngoại lệ phát sinh ở tầng service vào logger ERROR_AUDIT. */
    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logError(org.aspectj.lang.JoinPoint joinPoint, Throwable ex) {
        ERROR_AUDIT.error("[ERROR_AUDIT] method={} type={} message={}",
                joinPoint.getSignature().toShortString(), ex.getClass().getName(), ex.getMessage());
    }
}
