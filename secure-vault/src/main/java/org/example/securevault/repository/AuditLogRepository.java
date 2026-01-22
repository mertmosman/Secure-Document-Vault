package org.example.securevault.repository;

import org.example.securevault.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // Şimdilik özel bir sorguya ihtiyacımız yok, save() yeterli.
}