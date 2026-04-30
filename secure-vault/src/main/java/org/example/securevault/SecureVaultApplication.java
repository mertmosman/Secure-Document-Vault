package org.example.securevault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SecureVaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecureVaultApplication.class, args);
    }
}