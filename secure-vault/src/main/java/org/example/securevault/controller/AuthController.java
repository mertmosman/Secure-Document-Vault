package org.example.securevault.controller;

import org.example.securevault.dto.LoginRequest;
import org.example.securevault.dto.RegisterRequest;
import org.example.securevault.model.RefreshToken;
import org.example.securevault.model.User;
import org.example.securevault.repository.UserRepository;
import org.example.securevault.service.JwtService;
import org.example.securevault.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService; // YENİ: Refresh Token Servisi eklendi

    public AuthController(UserRepository userRepository,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserDetailsService userDetailsService,
                          RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
    }

    // REGISTER (Mevcut mantık korundu)
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Bu kullanıcı adı zaten alınmış.");
        }
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(request.getPassword());

        if (request.getRole() == null || request.getRole().isEmpty()) {
            newUser.setRole("ROLE_USER");
        } else {
            String role = request.getRole().toUpperCase();
            if (!role.startsWith("ROLE_")) role = "ROLE_" + role;
            newUser.setRole(role);
        }

        userRepository.save(newUser);
        return ResponseEntity.ok("Kullanıcı başarıyla oluşturuldu!");
    }

    // LOGIN - ARTIK 2 TOKEN BİRDEN DÖNÜYOR!
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        // 1. Spring Security ile kullanıcı adı ve şifreyi doğrula
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 2. Kullanıcı bilgilerini al
        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        // 3. (YENİ) Başka cihazda veya eski oturumda kalan refresh token'ları temizle
        refreshTokenService.deleteByUsername(request.getUsername());

        // 4. Tokenları üret
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(request.getUsername());

        // 5. Her ikisini de JSON objesi (Map) olarak dön
        Map<String, String> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken.getToken());

        return ResponseEntity.ok(response);
    }

    // YENİ: REFRESH ENDPOINT'İ (Access Token bitince buraya gelinecek)
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Map<String, String> request) {
        String requestRefreshToken = request.get("refreshToken");

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration) // Süresi dolmuş mu?
                .map(RefreshToken::getUser) // Dolmamışsa kullanıcıyı al
                .map(user -> {
                    // Kullanıcı için UserDetails oluştur
                    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

                    // Yepyeni bir Access Token ver
                    String newAccessToken = jwtService.generateToken(userDetails);

                    Map<String, String> response = new HashMap<>();
                    response.put("accessToken", newAccessToken);
                    response.put("refreshToken", requestRefreshToken); // Eskisini geri veriyoruz
                    return ResponseEntity.ok(response);
                })
                .orElseThrow(() -> new RuntimeException("Refresh token veritabanında bulunamadı veya süresi dolmuş!"));
    }
}