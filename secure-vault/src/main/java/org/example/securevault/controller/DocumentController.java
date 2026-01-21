package org.example.securevault.controller;

import org.example.securevault.model.Document;
import org.example.securevault.service.DocumentService;
import org.example.securevault.service.RateLimitingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final RateLimitingService rateLimitingService; // 1. YENİ SERVİSİ EKLE

    // Constructor'ı güncelle
    public DocumentController(DocumentService documentService, RateLimitingService rateLimitingService) {
        this.documentService = documentService;
        this.rateLimitingService = rateLimitingService;
    }

    // UPLOAD Endpoint - GÜNCELLENDİ
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file,
                                                 Principal principal) {
        String username = principal.getName();

        // --- 2. HIZ SINIRI KONTROLÜ BAŞLANGIÇ ---
        Bucket bucket = rateLimitingService.resolveBucket(username);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1); // 1 Jeton harcamayı dene

        if (!probe.isConsumed()) {
            // Eğer jeton yetmediyse:
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000; // Saniye cinsinden bekleme süresi
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS) // 429 Hatası
                    .body("Çok hızlı işlem yapıyorsunuz! Lütfen " + waitForRefill + " saniye bekleyin.");
        }
        // --- HIZ SINIRI KONTROLÜ BİTİŞ ---

        try {
            Document savedDoc = documentService.uploadFile(file, username);
            return ResponseEntity.ok("Dosya yüklendi. ID: " + savedDoc.getId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Yükleme Hatası: " + e.getMessage());
        }
    }

    // GET (ONE): ID ile getir (Service katmanında @PostAuthorize koruması var)
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable Long id) {
        // IDOR kontrolü Service içinde yapıldığı için burada ekstra koda gerek yok.
        // Eğer yetkisiz biri isterse Service otomatik 403 fırlatır.
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    // GET (ALL): Sadece giriş yapanın dosyalarını listele
    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments(Principal principal) {
        return ResponseEntity.ok(documentService.getAllDocuments(principal.getName()));
    }

    // DELETE: Giriş yapan kişi sadece kendi dosyasını silebilir
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable Long id, Principal principal) {
        try {
            documentService.deleteDocument(id, principal.getName());
            return ResponseEntity.ok("Dosya başarıyla silindi.");
        } catch (Exception e) {
            // AccessDeniedException servisten gelirse burası yakalar (veya GlobalExceptionHandler)
            return ResponseEntity.status(403).body("İşlem Başarısız: " + e.getMessage());
        }
    }
}