package org.example.securevault.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users") // 'user' PostgreSQL'de rezerve kelime olabilir, 'users' yapıyoruz.
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // Hashlenmiş şifre tutacağız

    private String role; // ROLE_USER, ROLE_ADMIN vs.
}
