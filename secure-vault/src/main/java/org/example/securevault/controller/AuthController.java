package org.example.securevault.controller;

import org.example.securevault.dto.LoginRequest;
import org.example.securevault.dto.RegisterRequest;
import org.example.securevault.model.User;
import org.example.securevault.repository.UserRepository;
import org.example.securevault.service.JwtService; // Eklendi
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager; // Eklendi
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Eklendi
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager; // Kullanıcıyı doğrulayan araç
    private final JwtService jwtService; // Token üreten araç
    private final UserDetailsService userDetailsService;

    public AuthController(UserRepository userRepository,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    // REGISTER (Aynı kalabilir, sadece rol mantığını koru)
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

    // LOGIN - ARTIK TOKEN DÖNÜYOR!
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        // 1. Spring Security ile kullanıcı adı ve şifreyi doğrula
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 2. Eğer buraya geldiyse giriş başarılıdır. Kullanıcı bilgilerini al.
        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        // 3. Token üret ve kullanıcıya ver.
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(token);
    }
}