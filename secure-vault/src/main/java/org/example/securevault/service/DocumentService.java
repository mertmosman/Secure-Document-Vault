package org.example.securevault.service;

import org.example.securevault.dto.FileProcessMessage;
import org.example.securevault.model.Document;
import org.example.securevault.model.DocumentStatus;
import org.example.securevault.model.User;
import org.example.securevault.repository.DocumentRepository;
import org.example.securevault.repository.UserRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final MinioStorageService minioStorageService;
    private final RabbitTemplate rabbitTemplate;

    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository,
                           MinioStorageService minioStorageService,
                           RabbitTemplate rabbitTemplate) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.minioStorageService = minioStorageService;
        this.rabbitTemplate = rabbitTemplate;
    }

    // CREATE: Asenkron Dosya Yükleme
    // YENİ: Hem userDocuments hem de allDocuments (Admin listesi) temizlenir
    @CacheEvict(value = {"userDocuments", "allDocuments"}, allEntries = true)
    public Document uploadFile(MultipartFile file, String username) throws Exception {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + username));

        String objectKey = minioStorageService.uploadFile(file);

        Document document = new Document();
        document.setFileName(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setObjectKey(objectKey);
        document.setUploadDate(LocalDateTime.now());
        document.setOwner(user);
        document.setStatus(DocumentStatus.PENDING);

        Document savedDoc = documentRepository.save(document);

        FileProcessMessage message = new FileProcessMessage(savedDoc.getId(), objectKey);
        rabbitTemplate.convertAndSend("file.process.queue", message);

        return savedDoc;
    }

    // READ (ONE): Metadata Getir
    @Transactional(readOnly = true)
    @PostAuthorize("returnObject.owner.username == authentication.name || hasRole('ROLE_ADMIN')")
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı ID: " + id));
    }

    // READ (ALL - USER): Sadece Kişinin Kendi Dosyaları
    @Cacheable(value = "userDocuments", key = "#username")
    public List<Document> getAllDocuments(String username) {
        System.out.println("⚠️ Veritabanına gidiliyor... (User araması)");
        return documentRepository.findAllByOwner_Username(username);
    }

    // READ (ALL - ADMIN): Sistemdeki Tüm Dosyalar
    // YENİ: Admin için çalışacak metot
    @Cacheable(value = "allDocuments")
    public List<Document> getAllDocumentsForAdmin() {
        System.out.println("⚠️ Veritabanına gidiliyor... (Admin araması - Tüm sistem)");
        return documentRepository.findAll();
    }

    // DELETE: Dosya Silme
    // YENİ: Silme işleminde de her iki önbellek temizlenir
    @CacheEvict(value = {"userDocuments", "allDocuments"}, allEntries = true)
    public void deleteDocument(Long id, String username) throws Exception {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı ID: " + id));

        if (!document.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("Bu dosyayı silme yetkiniz yok! Sadece kendi dosyanızı silebilirsiniz.");
        }

        minioStorageService.deleteFile(document.getObjectKey());
        documentRepository.deleteById(id);
    }
}