package org.example.securevault.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data // YENİ: Getter/Setter'ları otomatik oluşturur
@NoArgsConstructor // YENİ: Boş constructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String action;
    private String details;
    private String ipAddress;

    // YENİ: Teknik loglarla (ELK) eşleşmeyi sağlayacak sihirli kolon!
    private String correlationId;

    private LocalDateTime timestamp;

    // Hızlı kayıt için Constructor
    public AuditLog(String username, String action, String details, String ipAddress, String correlationId) {
        this.username = username;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.correlationId = correlationId; // YENİ
        this.timestamp = LocalDateTime.now();
    }
}