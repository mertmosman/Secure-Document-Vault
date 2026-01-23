package org.example.securevault.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    // Bu, Base64 formatında geçerli, 256-bitlik rastgele bir anahtardır.
    private final String TEST_SECRET_KEY = "c2VjdXJlLXZhdWx0LXByb2plY3Qtc2VjcmV0LWtleS12YWx1ZS0xMjM0NTY=";
    private final long TEST_EXPIRATION = 1000 * 60 * 60; // 1 Saat

    @BeforeEach
    void setUp() {
        // JwtService içindeki @Value ile okunan private değişkenlere
        // dışarıdan (Reflection ile) değer atıyoruz. Çünkü test ortamında Spring çalışmıyor.
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_EXPIRATION);
    }

    @Test
    void kullaniciIcinTokenUretip_DogruKullaniciAdiDonuyorMu() {
        // 1. HAZIRLIK (GIVEN)
        UserDetails user = new User("test_ajan", "sifre123", new ArrayList<>());

        // 2. İŞLEM (WHEN)
        String token = jwtService.generateToken(user);

        // 3. KONTROL (THEN)
        assertNotNull(token); // Token null olmamalı
        assertEquals("test_ajan", jwtService.extractUsername(token)); // Token'dan çıkan isim doğru mu?
    }

    @Test
    void tokenGecerliligiDogruKontrolEdiliyorMu() {
        // 1. HAZIRLIK
        UserDetails user = new User("super_admin", "123", new ArrayList<>());
        String token = jwtService.generateToken(user);

        // 2. KONTROL - Doğru Kullanıcı
        boolean isValid = jwtService.isTokenValid(token, user);
        assertTrue(isValid, "Doğru kullanıcı için token geçerli olmalı");

        // 3. KONTROL - Yanlış Kullanıcı
        UserDetails hacker = new User("hacker", "123", new ArrayList<>());
        boolean isHackerValid = jwtService.isTokenValid(token, hacker);
        assertFalse(isHackerValid, "Başkasının tokenı hacker için geçersiz olmalı");
    }

    @Test
    void suresiGecmisTokenHataliOlmali() {
        // Bu test için özel bir token üretelim (Süresi geçmiş)
        UserDetails user = new User("eski_kullanici", "123", new ArrayList<>());

        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET_KEY);
        Key key = Keys.hmacShaKeyFor(keyBytes);

        String expiredToken = Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // 1 saat önce üretildi
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60)) // 1 dakika önce bitti
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // ExpiredJwtException fırlatmasını bekliyoruz
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> {
            jwtService.isTokenValid(expiredToken, user);
        });
    }
}