package org.example.securevault.config;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.example.securevault.model.AuditLog;
import org.example.securevault.repository.AuditLogRepository;
import org.slf4j.MDC; // YENİ EKLENDİ
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Principal;

@Aspect
@Component
public class AuditLoggingAspect {

    private final AuditLogRepository auditLogRepository;

    public AuditLoggingAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @AfterReturning(pointcut = "execution(* org.example.securevault.controller.DocumentController.uploadDocument(..))")
    public void logUpload(JoinPoint joinPoint) {
        saveLog(joinPoint, "DOSYA_YUKLEME");
    }

    @AfterReturning(pointcut = "execution(* org.example.securevault.controller.DocumentController.deleteDocument(..))")
    public void logDelete(JoinPoint joinPoint) {
        saveLog(joinPoint, "DOSYA_SILME");
    }

    private void saveLog(JoinPoint joinPoint, String action) {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String ipAddress = request.getRemoteAddr();

            String username = "Bilinmiyor";
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof Principal) {
                    username = ((Principal) arg).getName();
                    break;
                }
            }

            // YENİ: Sistemin görünmez sırt çantasından Correlation ID'yi çek!
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = "Sistem-Tetiklemesi"; // Manuel testler vb. için önlem
            }

            String details = "Metot: " + joinPoint.getSignature().getName();

            // D. Veritabanına Yaz (Correlation ID ile birlikte)
            AuditLog log = new AuditLog(username, action, details, ipAddress, correlationId);
            auditLogRepository.save(log);

            System.out.println("--- AUDIT LOG KAYDEDİLDİ: " + action + " (Trace ID: " + correlationId + ") ---");

        } catch (Exception e) {
            System.out.println("Audit Log Hatası: " + e.getMessage());
        }
    }
}