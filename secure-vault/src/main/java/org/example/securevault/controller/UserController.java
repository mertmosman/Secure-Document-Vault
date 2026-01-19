package org.example.securevault.controller;

import org.example.securevault.model.User;
import org.example.securevault.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Rol kontrolü için
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GÜVENLİK: Bu metodu sadece veritabanında rolü 'ROLE_ADMIN' olanlar çalıştırabilir.
    // Normal kullanıcılar buraya erişirse 403 alır.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Tek Kullanıcı Getir (IDOR Korumalı)
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id, Principal principal) {
        // Servise "Ben kimim?" bilgisini gönderiyoruz
        return ResponseEntity.ok(userService.getUserById(id, principal.getName()));
    }

    // Kullanıcı Sil (IDOR Korumalı)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id, Principal principal) {
        try {
            userService.deleteUser(id, principal.getName());
            return ResponseEntity.ok("Hesap başarıyla silindi.");
        } catch (Exception e) {
            return ResponseEntity.status(403).body("İşlem Başarısız: " + e.getMessage());
        }
    }
}