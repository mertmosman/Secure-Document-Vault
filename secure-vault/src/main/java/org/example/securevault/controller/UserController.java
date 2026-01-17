package org.example.securevault.controller;

import org.example.securevault.model.User;
import org.example.securevault.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 1. READ (ALL): Tüm kullanıcıları listele (Admin paneli gibi düşün)
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // 2. READ (ONE): ID ile tek kullanıcı getir
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. UPDATE: Kullanıcı rolünü veya ismini güncelle
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        return userRepository.findById(id).map(existingUser -> {
            existingUser.setUsername(userDetails.getUsername());
            existingUser.setRole(userDetails.getRole());
            // Şifre güncelleme daha karmaşık olduğu için şimdilik girmiyoruz
            return ResponseEntity.ok(userRepository.save(existingUser));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 4. DELETE: Kullanıcıyı sil
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.ok("Kullanıcı silindi. ID: " + id);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}