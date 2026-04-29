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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final MinioStorageService minioStorageService;
    private final RabbitTemplate rabbitTemplate; // RABBITMQ EKLENDİ

    // DİKKAT: FileValidator'ı buradan sildik çünkü taramayı artık arka planda İşçi (Worker) yapacak!
    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository,
                           MinioStorageService minioStorageService,
                           RabbitTemplate rabbitTemplate) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.minioStorageService = minioStorageService;
        this.rabbitTemplate = rabbitTemplate;
    }

    // CREATE: Asenkron Dosya Yükleme (Kullanıcı Beklemez)
    public Document uploadFile(MultipartFile file, String username) throws Exception {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + username));

        // 1. Tika taraması YOK! Doğrudan MinIO'ya koyuyoruz (Milisaniyeler sürer)
        String objectKey = minioStorageService.uploadFile(file);

        // 2. Veritabanına "PENDING" (Bekliyor) olarak kaydet
        Document document = new Document();
        document.setFileName(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setObjectKey(objectKey);
        document.setUploadDate(LocalDateTime.now());
        document.setOwner(user);
        document.setStatus(DocumentStatus.PENDING); // YENİ STATÜ: İşlem Sırasında

        Document savedDoc = documentRepository.save(document);

        // 3. Postacıya (RabbitMQ) not bırak: "İşçi uyan, tarayacağın bir dosya var!"
        FileProcessMessage message = new FileProcessMessage(savedDoc.getId(), objectKey);
        rabbitTemplate.convertAndSend("file.process.queue", message);

        // 4. Kullanıcıya ANINDA cevap dön (Sunucu beklemez)
        return savedDoc;
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
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı ID: " + id));

        if (!document.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("Bu dosyayı silme yetkiniz yok! Sadece kendi dosyanızı silebilirsiniz.");
        }

        // Dosyayı MinIO'dan fiziksel olarak sil
        minioStorageService.deleteFile(document.getObjectKey());

        // Veritabanından sil
        documentRepository.deleteById(id);
    }
}