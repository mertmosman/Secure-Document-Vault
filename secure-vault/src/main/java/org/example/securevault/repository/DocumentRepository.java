package org.example.securevault.repository;

import org.example.securevault.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Magic Method: SQL yazmadan isimlendirme kuralıyla filtreleme yapar.
    // SELECT * FROM documents WHERE owner.username = ?
    List<Document> findAllByOwner_Username(String username);
}