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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
    private final MinioStorageService minioStorageService;

    public DocumentController(DocumentService documentService,
                              RateLimitingService rateLimitingService,
                              MinioStorageService minioStorageService) {
        this.documentService = documentService;
        this.rateLimitingService = rateLimitingService;
        this.minioStorageService = minioStorageService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file,
                                                 Principal principal) {
        String username = principal.getName();
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

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentInfo(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            InputStream stream = minioStorageService.downloadFile(document.getObjectKey());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // YENİ: ADMIN KONTROLÜ EKLENDİ
    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments(Authentication authentication) {
        // Kişinin rollerine bak, içinde ADMIN var mı?
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ADMIN"));

        if (isAdmin) {
            // Adminse bütün veritabanını getir
            return ResponseEntity.ok(documentService.getAllDocumentsForAdmin());
        } else {
            // User ise sadece kendininkileri getir
            return ResponseEntity.ok(documentService.getAllDocuments(authentication.getName()));
        }
    }

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