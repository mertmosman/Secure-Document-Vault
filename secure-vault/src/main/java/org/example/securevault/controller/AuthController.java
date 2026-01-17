package org.example.securevault.controller;

import org.example.securevault.model.User;
import org.example.securevault.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        // Basit bir kontrol: Bu kullanıcı adı alınmış mı?
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Bu kullanıcı adı zaten alınmış.");
        }

        // ŞİMDİLİK şifreyi düz (plain text) kaydediyoruz.
        // Güvenlik gününde (Gün 5) buraya şifreleme (BCrypt) ekleyeceğiz.
        user.setRole("ROLE_USER");
        userRepository.save(user);

        return ResponseEntity.ok("Kullanıcı başarıyla oluşturuldu!");
    }
}