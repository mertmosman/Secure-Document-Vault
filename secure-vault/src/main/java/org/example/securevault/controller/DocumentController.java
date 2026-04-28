package org.example.securevault.controller;

import org.example.securevault.model.Document;
import org.example.securevault.service.DocumentService;
import org.example.securevault.service.MinioStorageService;
import org.example.securevault.service.RateLimitingService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import java.io.InputStream;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final RateLimitingService rateLimitingService;
    private final MinioStorageService minioStorageService; // YENİ SERVİS EKLENDİ

    public DocumentController(DocumentService documentService,
                              RateLimitingService rateLimitingService,
                              MinioStorageService minioStorageService) {
        this.documentService = documentService;
        this.rateLimitingService = rateLimitingService;
        this.minioStorageService = minioStorageService;
    }

    // UPLOAD Endpoint
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file,
                                                 Principal principal) {
        String username = principal.getName();

        // --- HIZ SINIRI KONTROLÜ ---
        Bucket bucket = rateLimitingService.resolveBucket(username);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Çok hızlı işlem yapıyorsunuz! Lütfen " + waitForRefill + " saniye bekleyin.");
        }

        try {
            Document savedDoc = documentService.uploadFile(file, username);
            return ResponseEntity.ok("Dosya başarıyla MinIO'ya yüklendi. ID: " + savedDoc.getId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Yükleme Hatası: " + e.getMessage());
        }
    }

    // GET (METADATA): Sadece dosya bilgilerini (JSON) getirir
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentInfo(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    // GET (DOWNLOAD): Dosyanın kendisini fiziksel olarak indirir (YENİ EKLENDİ)
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            // 1. Güvenli şekilde DB'den dosya bilgilerini çek (IDOR korumalı)
            Document document = documentService.getDocumentById(id);

            // 2. MinIO'dan asıl veri akışını (Stream) al
            InputStream stream = minioStorageService.downloadFile(document.getObjectKey());

            // 3. İndirilebilir formatta (Attachment) dön
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                    .body(new InputStreamResource(stream));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // GET (ALL): Sadece giriş yapanın dosyalarını listele
    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments(Principal principal) {
        return ResponseEntity.ok(documentService.getAllDocuments(principal.getName()));
    }

    // DELETE: Dosyayı hem DB'den hem MinIO'dan siler
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable Long id, Principal principal) {
        try {
            documentService.deleteDocument(id, principal.getName());
            return ResponseEntity.ok("Dosya başarıyla veritabanından ve MinIO'dan silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(403).body("İşlem Başarısız: " + e.getMessage());
        }
    }
}