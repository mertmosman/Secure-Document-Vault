package org.example.securevault.service;

import org.example.securevault.model.Document;
import org.example.securevault.model.User;
import org.example.securevault.repository.DocumentRepository;
import org.example.securevault.repository.UserRepository;
import org.example.securevault.validation.FileValidator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    private final String UPLOAD_DIR = "uploads/";

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileValidator fileValidator;

    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository,
                           FileValidator fileValidator) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.fileValidator = fileValidator;
    }

    // CREATE: Dosya Yükleme
    public Document uploadFile(MultipartFile file, String username) throws IOException {
        // 1. Güvenlik Kontrolü (Tika & Uzantı)
        fileValidator.validateFile(file);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + username));

        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String filePath = UPLOAD_DIR + originalFilename;
        Path path = Paths.get(filePath);
        Files.write(path, file.getBytes());

        Document document = new Document();
        document.setFileName(originalFilename);
        document.setFileType(file.getContentType());
        document.setFilePath(filePath);
        document.setUploadDate(LocalDateTime.now());
        document.setOwner(user); // Dosyayı kullanıcıya zimmetle

        return documentRepository.save(document);
    }

    // READ (ONE): Tek Dosya Getir
    // GÜVENLİK: Metot çalışır, veriyi çeker ama dönmeden önce sahibini kontrol eder.
    // Dosyanın sahibi (returnObject.owner.username == authentication.name)
    // VEYA (||)
    // İsteyen kişi Admin ise (hasRole('ROLE_ADMIN'))
    @PostAuthorize("returnObject.owner.username == authentication.name || hasRole('ROLE_ADMIN')")
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı ID: " + id));
    }

    // READ (ALL): Sadece Kişinin Kendi Dosyaları
    public List<Document> getAllDocuments(String username) {
        return documentRepository.findAllByOwner_Username(username);
    }

    // DELETE: Dosya Silme (Güvenli)
    public void deleteDocument(Long id, String username) throws IOException {
        // 1. Dosyayı bul
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı ID: " + id));

        // 2. GÜVENLİK KONTROLÜ: Silmek isteyen kişi, dosyanın sahibi mi?
        if (!document.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("Bu dosyayı silme yetkiniz yok! Sadece kendi dosyanızı silebilirsiniz.");
        }

        // 3. Diskteki dosyayı sil
        Path path = Paths.get(document.getFilePath());
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.out.println("Dosya diskte bulunamadı, DB temizleniyor.");
        }

        // 4. Veritabanından sil
        documentRepository.deleteById(id);
    }
}