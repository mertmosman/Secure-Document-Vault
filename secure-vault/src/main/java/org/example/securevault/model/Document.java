package org.example.securevault.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    private String fileName; // orjinal dosya adı (tez.pdf)

    private String fileType; // application/pdf (MIME type)

    // YENİ: Dosyanın MinIO üzerindeki adresi (UUID_dosyaadi)
    private String objectKey;

    private LocalDateTime uploadDate;

    // Dosyanın kime ait olduğunu bilmeliyiz
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;
}