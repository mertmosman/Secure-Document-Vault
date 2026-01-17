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
    // LOGIN (Basit Kontrol)
    // Şimdilik Token vermiyoruz, sadece "Giriş Başarılı" diyoruz.
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User loginRequest) {
        // 1. Kullanıcıyı bul
        User dbUser = userRepository.findByUsername(loginRequest.getUsername())
                .orElse(null);

        // 2. Kullanıcı yoksa veya şifre yanlışsa hata dön
        if (dbUser == null || !dbUser.getPassword().equals(loginRequest.getPassword())) {
            return ResponseEntity.status(401).body("Giriş Başarısız: Kullanıcı adı veya şifre hatalı!");
        }

        // 3. Her şey doğruysa
        return ResponseEntity.ok("Giriş Başarılı! Hoşgeldin " + dbUser.getUsername());
    }
}