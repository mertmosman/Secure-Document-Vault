package org.example.securevault.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class RateLimitingService {

    private final StringRedisTemplate redisTemplate;
    // Kullanıcı dakikada sadece 5 dosya yükleyebilir
    private static final int MAX_REQUESTS_PER_MINUTE = 5;

    public RateLimitingService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String username) {
        // Redis'te bu kullanıcıya özel bir çekmece (key) açıyoruz
        String key = "rate_limit:upload:" + username;

        // Redis'teki sayacı 1 artır (Eğer yoksa oluşturur ve 1 yapar)
        Long currentRequests = redisTemplate.opsForValue().increment(key);

        // Eğer bu ilk istekse, bu sayacın ömrünü (TTL) 1 dakika olarak ayarla
        // 1 dakika sonra Redis bu sayacı otomatik olarak çöpe atacak ve limit sıfırlanacak.
        if (currentRequests != null && currentRequests == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        // Eğer limit aşıldıysa false dön (İzin verme)
        return currentRequests != null && currentRequests <= MAX_REQUESTS_PER_MINUTE;
    }
}