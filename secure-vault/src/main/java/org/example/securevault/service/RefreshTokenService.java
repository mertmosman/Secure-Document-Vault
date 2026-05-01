package org.example.securevault.service;

import org.example.securevault.model.RefreshToken;
import org.example.securevault.repository.RefreshTokenRepository;
import org.example.securevault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public RefreshToken createRefreshToken(String username) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(userRepository.findByUsername(username).orElseThrow());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString()); // Şifreli JWT değil, rastgele UUID yapıyoruz

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

    @Transactional
    public int deleteByUsername(String username) {
        return refreshTokenRepository.deleteByUser(userRepository.findByUsername(username).orElseThrow());
    }
}