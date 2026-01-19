package org.example.securevault.config;

import org.example.securevault.model.User;
import org.example.securevault.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Veritabanında "admin" kullanıcısı var mı diye bak
        if (userRepository.findByUsername("admin").isEmpty()) {
            System.out.println("--- BAŞLANGIÇ ADMİNİ OLUŞTURULUYOR ---");

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("123"); // Şifre: 123
            admin.setRole("ROLE_ADMIN"); // DİKKAT: ROLE_ ön eki önemli!

            userRepository.save(admin);

            System.out.println("--- ADMIN OLUŞTURULDU: (Kullanıcı: admin / Şifre: 123) ---");
        }
    }
}