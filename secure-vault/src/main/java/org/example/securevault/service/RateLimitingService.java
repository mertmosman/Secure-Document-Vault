package org.example.securevault.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // Her kullanıcı (username) için bir kova saklayacağız (Memory Cache)
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Kullanıcının kovasını getir (Yoksa oluştur)
    public Bucket resolveBucket(String username) {
        return cache.computeIfAbsent(username, this::createNewBucket);
    }

    private Bucket createNewBucket(String username) {
        // KURAL: Dakikada 2 hak ver. (Test etmesi kolay olsun diye az veriyoruz)
        // Refill.greedy(2, Duration.ofMinutes(1)) -> Dakikada 2 jeton ekle.
        Bandwidth limit = Bandwidth.classic(2, Refill.greedy(2, Duration.ofMinutes(1)));

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}