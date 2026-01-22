package org.example.securevault.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;    // İşlemi yapan kim?
    private String action;      // Ne yaptı? (UPLOAD, DELETE)
    private String details;     // Detay (Dosya ID: 5)
    private String ipAddress;   // Hangi IP'den?
    private LocalDateTime timestamp; // Ne zaman?

    // Boş Constructor
    public AuditLog() {}

    // Hızlı kayıt için Constructor
    public AuditLog(String username, String action, String details, String ipAddress) {
        this.username = username;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
    }

    // Getter ve Setter'lar (Lombok kullanıyorsan @Data ekle, yoksa bunları elle oluştur)
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getTimestamp() { return timestamp; }
}