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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileValidator fileValidator;
    private final MinioStorageService minioStorageService; // YENİ SERVİS EKLENDİ

    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository,
                           FileValidator fileValidator,
                           MinioStorageService minioStorageService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.fileValidator = fileValidator;
        this.minioStorageService = minioStorageService;
    }

    // CREATE: Dosya Yükleme (MinIO Entegreli)
    public Document uploadFile(MultipartFile file, String username) throws Exception {
        // 1. Güvenlik Kontrolü (Tika & Uzantı)
        fileValidator.validateFile(file);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + username));

        // 2. Dosyayı fiziksel olarak MinIO'ya yükle ve adresi (key) al
        String objectKey = minioStorageService.uploadFile(file);

        // 3. Veritabanına sadece Metadata'yı (bilgileri) kaydet
        Document document = new Document();
        document.setFileName(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setObjectKey(objectKey); // MinIO adresi
        document.setUploadDate(LocalDateTime.now());
        document.setOwner(user); // Dosyayı kullanıcıya zimmetle

        return documentRepository.save(document);
    }

    // READ (ONE): Metadata Getir (Güvenli)
    @PostAuthorize("returnObject.owner.username == authentication.name || hasRole('ROLE_ADMIN')")
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı ID: " + id));
    }

    // READ (ALL): Sadece Kişinin Kendi Dosyaları
    public List<Document> getAllDocuments(String username) {
        return documentRepository.findAllByOwner_Username(username);
    }

    // DELETE: Dosya Silme (Güvenli & MinIO Entegreli)
    public void deleteDocument(Long id, String username) throws Exception {
        // 1. Dosyayı bul
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı ID: " + id));

        // 2. GÜVENLİK KONTROLÜ
        if (!document.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("Bu dosyayı silme yetkiniz yok! Sadece kendi dosyanızı silebilirsiniz.");
        }

        // 3. Dosyayı MinIO'dan fiziksel olarak sil
        minioStorageService.deleteFile(document.getObjectKey());

        // 4. Veritabanından sil
        documentRepository.deleteById(id);
    }
}