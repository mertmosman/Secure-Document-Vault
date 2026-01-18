package org.example.securevault.controller;
import org.example.securevault.dto.LoginRequest;
import org.example.securevault.dto.RegisterRequest;
import org.example.securevault.model.User;
import org.example.securevault.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // SecurityConfig'deki encoder'ı kullanacağız

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // KAYIT OL (DTO kullanıyor: Role var)
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Bu kullanıcı adı zaten alınmış.");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        // Şifreyi şimdilik olduğu gibi alıyoruz (Gün 5'te hashleyeceğiz)
        newUser.setPassword(request.getPassword());
        newUser.setRole(request.getRole()); // Rolü DTO'dan alıp set ediyoruz

        userRepository.save(newUser);
        return ResponseEntity.ok("Kullanıcı başarıyla oluşturuldu!");
    }

    // GİRİŞ YAP (DTO kullanıyor: Role YOK)
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        User dbUser = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        // Şifre kontrolü
        if (dbUser == null || !dbUser.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(401).body("Giriş Başarısız: Kullanıcı adı veya şifre hatalı!");
        }

        return ResponseEntity.ok("Giriş Başarılı! Hoşgeldin " + dbUser.getUsername());
    }
}