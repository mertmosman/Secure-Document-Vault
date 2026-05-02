import http from 'k6/http';
import { check, sleep } from 'k6';

// TEST AYARLARI: Şov başlıyor!
export let options = {
    stages: [
        { duration: '10s', target: 20 },  // 10 saniye içinde kullanıcı sayısını yavaşça 20'ye çıkar (Isınma)
        { duration: '30s', target: 100 }, // 30 saniye boyunca tam 100 kişi aynı anda login olmaya çalışsın (Zirve)
        { duration: '10s', target: 0 },   // 10 saniye içinde yavaşça 0'a düş (Soğuma)
    ],
};

// HER BİR KULLANICININ YAPACAĞI İŞLEM
export default function () {
    // 1. İstek atacağımız URL
    const url = 'http://localhost:8081/api/auth/login';

    // 2. Gönderilecek JSON Verisi
    const payload = JSON.stringify({
        username: 'ahmet',
        password: 'ahmet'
    });

    // 3. İstek Başlıkları
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // 4. İsteği Ateşle!
    let res = http.post(url, payload, params);

    // 5. Kontrol Et: İstek başarılı oldu mu? (Token döndü mü?)
    check(res, {
        'Giris Basarili (Status 200)': (r) => r.status === 200,
        'Token Geldi': (r) => r.body ? r.body.includes('accessToken') : false,
    });

    // Saniyede bir nefes al (Sistemi tamamen çökertmemek için)
    sleep(1);
}