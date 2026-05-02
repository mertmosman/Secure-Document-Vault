package org.example.securevault.service;

import org.example.securevault.model.RefreshToken;
import org.example.securevault.repository.RefreshTokenRepository;
import org.example.securevault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.securevault.model.User;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    // Refresh Token süresi: 7 Gün (Milisaniye cinsinden)
    private final Long refreshTokenDurationMs = 604800000L;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user); // Veritabanına sormaya gerek kalmadı!
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        // Eğer token'ın süresi dolmuşsa, veritabanından sil ve hata fırlat
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token süresi dolmuş. Lütfen tekrar giriş yapın.");
        }
        return token;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // username yerine User objesi alsın
    @Transactional
    public int deleteByUser(User user) {
        return refreshTokenRepository.deleteByUser(user);
    }
}