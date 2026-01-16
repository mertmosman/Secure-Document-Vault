package org.example.securevault.service;

import org.example.securevault.model.Document;
import org.example.securevault.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
public class DocumentService {

    // Dosyaların kaydedileceği klasör yolu (Proje klasörünün içinde oluşacak)
    private final String UPLOAD_DIR = "uploads/";

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Document uploadFile(MultipartFile file, String uploaderName) throws IOException {
        // 1. Klasör yoksa oluştur
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 2. Dosya ismini al ve çakışmayı önlemek için basit bir işlem yap (Şimdilik orjinal isim)
        String originalFilename = file.getOriginalFilename();
        // Güvenlik Notu: Burada henüz "Path Traversal" kontrolü yapmıyoruz! (Güvensiz Hal)

        String filePath = UPLOAD_DIR + originalFilename;
        Path path = Paths.get(filePath);

        // 3. Dosyayı diske kaydet (byte byte yazar)
        Files.write(path, file.getBytes());

        // 4. Veritabanına kayıt at
        Document document = new Document();
        document.setFileName(originalFilename);
        document.setFileType(file.getContentType());
        document.setFilePath(filePath);
        document.setUploadDate(LocalDateTime.now());

        // Not: User işlemleri henüz devre dışı, o yüzden owner null kalabilir veya ileride bağlayacağız.

        return documentRepository.save(document);
    }
}
