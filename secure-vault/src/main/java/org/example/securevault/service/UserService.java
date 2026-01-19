package org.example.securevault.service;

import org.example.securevault.model.User;
import org.example.securevault.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder; // Şifre güncelleme için lazım olacak
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    // PasswordEncoder'ı şimdilik constructor'a eklemiyorum, login/update yaparken lazım olur.

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ADMIN ÖZEL: Tüm kullanıcıları getir
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // KİŞİYE ÖZEL: Kendi profilini getir
    public User getUserById(Long id, String loggedInUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı ID: " + id));

        // GÜVENLİK KONTROLÜ: İsteyen kişi, istenen kişi mi?
        // NOT: Eğer Admin ise geçiş izni verilebilir ama şimdilik katı kural uyguluyoruz.
        if (!user.getUsername().equals(loggedInUsername)) {
            throw new AccessDeniedException("Bu profili görüntüleme yetkiniz yok!");
        }

        return user;
    }

    // KİŞİYE ÖZEL: Kendi hesabını sil
    public void deleteUser(Long id, String loggedInUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı ID: " + id));

        // GÜVENLİK KONTROLÜ
        if (!user.getUsername().equals(loggedInUsername)) {
            throw new AccessDeniedException("Başkasına ait bir hesabı silemezsiniz!");
        }

        userRepository.deleteById(id);
    }

    // UPDATE metodu da benzer mantıkla eklenebilir.
}