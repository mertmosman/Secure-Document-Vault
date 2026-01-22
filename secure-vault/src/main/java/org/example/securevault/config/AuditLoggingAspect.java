package org.example.securevault.config;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.example.securevault.model.AuditLog;
import org.example.securevault.repository.AuditLogRepository;
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

    // 1. UPLOAD İşlemini Dinle
    // DocumentController içindeki uploadDocument metodu başarıyla bitince burası çalışır.
    @AfterReturning(pointcut = "execution(* org.example.securevault.controller.DocumentController.uploadDocument(..))")
    public void logUpload(JoinPoint joinPoint) {
        saveLog(joinPoint, "DOSYA_YUKLEME");
    }

    // 2. DELETE İşlemini Dinle
    @AfterReturning(pointcut = "execution(* org.example.securevault.controller.DocumentController.deleteDocument(..))")
    public void logDelete(JoinPoint joinPoint) {
        saveLog(joinPoint, "DOSYA_SILME");
    }

    // Ortak Kayıt Metodu
    private void saveLog(JoinPoint joinPoint, String action) {
        try {
            // A. İstek bilgilerini al (IP adresi vb. için)
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String ipAddress = request.getRemoteAddr();

            // B. Kullanıcı adını bul (Parametrelerden)
            String username = "Bilinmiyor";
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof Principal) {
                    username = ((Principal) arg).getName();
                    break;
                }
            }

            // C. Detay oluştur (Hangi metot çalıştı?)
            String details = "Metot: " + joinPoint.getSignature().getName();

            // D. Veritabanına Yaz
            AuditLog log = new AuditLog(username, action, details, ipAddress);
            auditLogRepository.save(log);

            System.out.println("--- AUDIT LOG KAYDEDİLDİ: " + action + " ---");

        } catch (Exception e) {
            System.out.println("Audit Log Hatası: " + e.getMessage());
        }
    }
}