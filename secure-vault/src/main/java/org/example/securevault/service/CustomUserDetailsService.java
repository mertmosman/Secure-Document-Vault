package org.example.securevault.service; // Paket ismin neyse o kalsın

import org.example.securevault.model.User;
import org.example.securevault.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // 1. BU IMPORT ŞART
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service; // 2. BU IMPORT ŞART

@Service // <--- 3. İŞTE HATANIN ÇÖZÜMÜ BURASI! (Bu yoksa o hatayı alırsın)
public class CustomUserDetailsService implements UserDetailsService { // <--- 4. BU DA ŞART

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().replace("ROLE_", "")) // Spring bazen ROLE_ kısmını kendi ekler, temizliyoruz.
                .build();
    }
}