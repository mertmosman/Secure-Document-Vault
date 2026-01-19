package org.example.securevault.controller;

import org.example.securevault.model.Document;
import org.example.securevault.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // UPLOAD: Giriş yapan kişi adına yükler
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file,
                                                 Principal principal) {
        try {
            String loggedInUser = principal.getName();
            Document savedDoc = documentService.uploadFile(file, loggedInUser);
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