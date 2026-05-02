package org.example.securevault.controller;

import org.example.securevault.model.Document;
import org.example.securevault.service.DocumentService;
import org.example.securevault.service.MinioStorageService;
import org.example.securevault.service.RateLimitingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // --- REDİS DAĞITIK HIZ SINIRI KONTROLÜ ---
        if (!rateLimitingService.isAllowed(username)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Çok hızlı işlem yapıyorsunuz! Lütfen 1 dakika bekleyin.");
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

    // YENİ: GET (DOWNLOAD LINK): Artık dosyayı değil, 5 dakikalık MinIO indirme linkini dönüyor!
    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> getDownloadLink(@PathVariable Long id, Authentication authentication) {

        String username = authentication.getName();

        // Kullanıcı Admin mi kontrolü
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN") || role.getAuthority().equals("ADMIN"));

        // Servisten imzalı linki üret
        String downloadUrl = documentService.generateDownloadLink(id, username, isAdmin);

        // Frontend'e JSON olarak url'i ver
        Map<String, String> response = new HashMap<>();
        response.put("downloadUrl", downloadUrl);
        response.put("expiresIn", "5 Minutes");

        return ResponseEntity.ok(response);
    }

    // ADMIN KONTROLÜ
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