package org.example.securevault.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
public class Document implements Serializable { // YENİ: Redis için eklendi

    private static final long serialVersionUID = 1L; // Güvenlik standardı

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    private String fileName;
    private String fileType;
    private String objectKey;
    private LocalDateTime uploadDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    // YENİ: Tembel yüklemenin Redis'i (Jackson) çökertmesini engeller
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User owner;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;
}