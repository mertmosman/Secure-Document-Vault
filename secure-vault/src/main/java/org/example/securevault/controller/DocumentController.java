package org.example.securevault.controller;

import org.example.securevault.model.Document;
import org.example.securevault.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // CREATE (Upload) - Zaten vardı
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file,
                                                 @RequestParam("username") String username) {
        try {
            Document savedDoc = documentService.uploadFile(file, username);
            return ResponseEntity.ok("Dosya yüklendi. ID: " + savedDoc.getId());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Hata: " + e.getMessage());
        }
    }

    // READ (ONE) - Zaten vardı
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    // READ (ALL) - YENİ: Hepsini gör
    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    // DELETE - YENİ: Dosyayı yok et
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return ResponseEntity.ok("Dosya başarıyla silindi (Disk + DB).");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Silme hatası: " + e.getMessage());
        }
    }
}
