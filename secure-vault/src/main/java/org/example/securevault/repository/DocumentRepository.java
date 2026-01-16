package org.example.securevault.repository;
import org.example.securevault.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwnerId(Long userId); // Bir kullanıcının dosyalarını getirmek için
}
