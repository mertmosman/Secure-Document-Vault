package org.example.securevault.service;

import org.example.securevault.dto.FileProcessMessage;
import org.example.securevault.model.Document;
import org.example.securevault.model.DocumentStatus;
import org.example.securevault.repository.DocumentRepository;
import org.example.securevault.validation.FileValidator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class DocumentWorker {

    private final MinioStorageService minioStorageService;
    private final DocumentRepository documentRepository;
    private final FileValidator fileValidator;

    public DocumentWorker(MinioStorageService minioStorageService, DocumentRepository documentRepository, FileValidator fileValidator) {
        this.minioStorageService = minioStorageService;
        this.documentRepository = documentRepository;
        this.fileValidator = fileValidator;
    }

    // Bu metot, kuyruğa mesaj düştüğü anda OTOMATİK tetiklenir!
    @RabbitListener(queues = "file.process.queue")
    public void processFile(FileProcessMessage message) {
        System.out.println("📥 İŞÇİ (WORKER) UYANDI! Mesaj alındı: " + message.getObjectKey());

        Document document = documentRepository.findById(message.getDocumentId()).orElse(null);
        if (document == null) return;

        try {
            // 1. Dosyayı MinIO'dan çek
            InputStream stream = minioStorageService.downloadFile(message.getObjectKey());

            // 2. Güvenlik Taraması (Tika ile)
            System.out.println("🔍 Dosya taranıyor: " + document.getFileName());
            fileValidator.validateStream(stream, document.getFileName());

            // 3. Tarama Başarılıysa DB'de durumu güncelle
            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);
            System.out.println("✅ Tarama Başarılı. Dosya güvenli: " + document.getFileName());

        } catch (Exception e) {
            // 4. Tarama Başarısızsa (Zararlı Dosya)
            System.out.println("❌ ZARARLI DOSYA TESPİT EDİLDİ! " + e.getMessage());

            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);

            // Cezayı kes: Dosyayı MinIO'dan anında yok et!
            try {
                minioStorageService.deleteFile(message.getObjectKey());
                System.out.println("🗑️ Zararlı dosya MinIO'dan silindi.");
            } catch (Exception ex) {
                System.out.println("Hata: Dosya MinIO'dan silinemedi.");
            }
        }
    }
}