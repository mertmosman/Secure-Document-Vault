package org.example.securevault.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // service paketindeki TÜM metotları izle demek
    @Around("execution(* org.example.securevault.service.*.*(..))")
    public Object logServiceExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.info("METOT BASLADI: {}.{}", className, methodName);

        try {
            // Asıl metodu çalıştır
            Object result = joinPoint.proceed();

            long elapsedTime = System.currentTimeMillis() - start;
            log.info("METOT BITTI: {}.{} [Sure: {} ms]", className, methodName, elapsedTime);

            return result;
        } catch (IllegalArgumentException e) {
            log.error("HATALI ARGUMAN: {}.{}() -> {}", className, methodName, e.getMessage());
            throw e;
        }
    }
}