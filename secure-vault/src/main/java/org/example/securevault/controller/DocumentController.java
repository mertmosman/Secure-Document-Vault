package org.example.securevault.controller;

import org.example.securevault.model.Document;
import org.example.securevault.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // consumes = "multipart/form-data" diyerek bunun dosya olduğunu belirtiyoruz
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file,
                                                 @RequestParam("username") String username) {
        try {
            Document savedDoc = documentService.uploadFile(file, username);
            return ResponseEntity.ok("Dosya başarıyla yüklendi. ID: " + savedDoc.getId());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Dosya yüklenirken hata oluştu: " + e.getMessage());
        }
    }
}
