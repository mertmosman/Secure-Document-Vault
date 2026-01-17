package org.example.securevault.service;

import org.example.securevault.model.Document;
import org.example.securevault.model.User;
import org.example.securevault.repository.DocumentRepository;
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

    private final String UPLOAD_DIR = "uploads/";
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository; // YENİ: User tablosuna erişim lazım

    // Constructor Injection ile UserRepository'yi de alıyoruz
    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    public Document uploadFile(MultipartFile file, String username) throws IOException {
        // 1. ÖNCE KULLANICIYI KONTROL ET (Validation)
        // Eğer kullanıcı yoksa işlem burada durur, hata fırlatır.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + username));

        // 2. Klasör ve Dosya İşlemleri (Aynı kalıyor)
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Benzersiz isim üretmek iyi pratiktir ama şimdilik orjinal kalsın
        String originalFilename = file.getOriginalFilename();
        String filePath = UPLOAD_DIR + originalFilename;
        Path path = Paths.get(filePath);
        Files.write(path, file.getBytes());

        // 3. Veritabanı Kaydı (GÜNCELLENDİ)
        Document document = new Document();
        document.setFileName(originalFilename);
        document.setFileType(file.getContentType());
        document.setFilePath(filePath);
        document.setUploadDate(LocalDateTime.now());

        document.setOwner(user); // İŞTE KRİTİK NOKTA: Dosyayı gerçek kullanıcıya bağlıyoruz.

        return documentRepository.save(document);
    }

    //: Dosya Bilgisini Getirme (READ)
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı ID: " + id));
    }
    // DELETE işlemi: Hem DB'den hem Diskten siler
    public void deleteDocument(Long id) throws IOException {
        // 1. Önce dosyayı bul
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dosya bulunamadı"));

        // 2. Diskteki dosyayı sil (Files.delete)
        Path path = Paths.get(document.getFilePath());
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.out.println("Dosya diskte bulunamadı ama DB'den silinecek: " + e.getMessage());
        }

        // 3. Veritabanından sil
        documentRepository.deleteById(id);
    }

    // READ (ALL): Tüm dosyaları listeleme (Test için)
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }
}
