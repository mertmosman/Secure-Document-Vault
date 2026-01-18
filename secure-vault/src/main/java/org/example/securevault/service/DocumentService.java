package org.example.securevault.service;

import org.example.securevault.model.Document;
import org.example.securevault.model.User;
import org.example.securevault.repository.DocumentRepository;
import org.example.securevault.validation.FileValidator;
import org.example.securevault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Service
public class DocumentService {

    // Dosyaların kaydedileceği klasör
    private final String UPLOAD_DIR = "uploads/";

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileValidator fileValidator; // Güvenlik Kontrolü için

    // Tüm bağımlılıkları (Repositoryler ve Validator) buradan alıyoruz
    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository,
                           FileValidator fileValidator) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.fileValidator = fileValidator;
    }

    // CREATE: Dosya Yükleme (Güvenli)
    public Document uploadFile(MultipartFile file, String username) throws IOException {
        // 1. ADIM: GÜVENLİK KONTROLÜ (Apache Tika)
        // Dosya gerçekten PDF mi? İçeriğine bakılır. Değilse hata fırlatır ve durur.
        fileValidator.validateFile(file);

        // 2. ADIM: Kullanıcıyı Bul
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + username));

        // 3. ADIM: Klasör Yoksa Oluştur
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 4. ADIM: Dosyayı Diske Kaydet
        String originalFilename = file.getOriginalFilename();
        // İpucu: İleride buraya UUID ekleyerek dosya ismini benzersiz yapacağız (tez.pdf -> tez_12345.pdf gibi)
        String filePath = UPLOAD_DIR + originalFilename;
        Path path = Paths.get(filePath);
        Files.write(path, file.getBytes());

        // 5. ADIM: Veritabanına Kaydet
        Document document = new Document();
        document.setFileName(originalFilename);
        document.setFileType(file.getContentType());
        document.setFilePath(filePath);
        document.setUploadDate(LocalDateTime.now());
        document.setOwner(user); // Dosyayı kullanıcıya zimmetle

        return documentRepository.save(document);
    }

    // READ (ONE): ID ile dosya getir
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı ID: " + id));
    }

    // READ (ALL): Tüm dosyaları listele
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    // DELETE: Dosyayı hem diskten hem veritabanından sil
    public void deleteDocument(Long id) throws IOException {
        // 1. Önce veritabanı kaydını bul
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı ID: " + id));

        // 2. Diskteki fiziksel dosyayı sil
        Path path = Paths.get(document.getFilePath());
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.out.println("Dosya diskte bulunamadı (zaten silinmiş olabilir), ama DB'den siliniyor.");
        }

        // 3. Veritabanı kaydını sil
        documentRepository.deleteById(id);
    }
}
