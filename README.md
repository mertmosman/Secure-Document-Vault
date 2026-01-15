# 📁 Proje: Secure Document Vault (Güvenli Belge Kasası)

## Hedef: Spring Boot ile OWASP standartlarına uygun, saldırıya dayanıklı bir REST API geliştirmek.

### 1. Senaryo ve Amaç
Bir hukuk bürosu, müşterilerine ait çok hassas dava dosyalarını (sözleşmeler, deliller, kimlikler) dijital ortamda saklamak istiyor. Ancak siber saldırıların arttığı bu dönemde, standart bir dosya yükleme sistemi onlar için yeterli değil.

Göreviniz: Java ve Spring Boot ekosistemini kullanarak, sadece "çalışan" değil, aynı zamanda siber saldırılara karşı "dirençli" bir REST API geliştirmektir. Bu sistemde kimse başkasının dosyasını görememeli, sunucuya virüslü dosya yüklenememeli ve sistem aşırı isteklerle (DDoS/Brute-Force) çökertilememelidir.

### 2. Teknik Gereksinimler (Tech Stack)

Dil: Java 17 veya 21 (Virtual Threads desteği için tercih sebebidir)

Framework: Spring Boot 3.2+

Veritabanı: PostgreSQL

Güvenlik: Spring Security 6 (Method Security aktif)

IO & Validation:

Apache Tika (Dosya tipi analizi için)

Java I/O (Manuel Magic Bytes kontrolü için)

Rate Limiting: Bucket4j

Utility: Lombok, Maven/Gradle

### 3. Fonksiyonel Gereksinimler

Auth: Kullanıcılar sisteme kayıt olabilmeli ve Token (JWT) alarak giriş yapabilmelidir.

Upload: Yetkili kullanıcı sisteme PDF formatında belge yükleyebilmelidir.

View: Kullanıcı, yüklediği belgenin detaylarını veya içeriğini ID ile sorgulayabilmelidir (GET /api/files/{id}).

### 4. Güvenli Kodlama Görevleri (Secure Coding Tasks)

🛡️ Görev 1: Yetkilendirme Kontrolü (Broken Access Control)
Problem: Standart bir yazılımda, User A giriş yaptığında, URL'deki ID'yi değiştirerek (/api/files/125) User B'nin dosyasını görüntüleyebilir (IDOR Açığı).

Yasak: Service katmanında if (file.getOwner().equals(currentUser)) gibi manuel if-else blokları yazmak yasaktır (Spagetti koda yol açar).


🛡️ Görev 2: Gerçek Dosya Tipi Doğrulaması (Malicious File Upload)
Problem: Saldırganlar, zararli_yazilim.exe dosyasının adını tez_odevi.pdf olarak değiştirip sisteme yükleyebilirler. Sadece dosya uzantısına (.pdf) bakmak yetersizdir.


🛡️ Görev 3: Hız Sınırlama (Rate Limiting)
Problem: Bir bot, saniyede 1000 istek göndererek sistemi kilitleyebilir (DoS Saldırısı).

Limit aşıldığında sistem veritabanına gitmeden HTTP 429 (Too Many Requests) hatası dönmelidir.

🛡️ Görev 4: Güvenli Hata Yönetimi (Security Misconfiguration) 🆕
Problem: Sistem hata verdiğinde (örneğin IDOR yakalandığında), Spring Boot varsayılan olarak "Stack Trace" (kodun hangi satırda hata verdiği) bilgisini döner. Bu, saldırganlara sistem hakkında ipucu verir.

### 5. Proje Dosya Yapısı Örneği
```
src/main/java/com/projeadı/securevault
│
├── config
│   ├── SecurityConfig.java      (Spring Security Ayarları)
│   └── BucketConfig.java        (Rate Limit Ayarları)
│
├── controller
│   ├── AuthController.java
│   └── DocumentController.java
│
├── exception
│   ├── GlobalExceptionHandler.java  (Görev 4: @ControllerAdvice burada)
│   └── FileStorageException.java
│
├── filter
│   └── RateLimitFilter.java     (Görev 3: Bucket4j burada)
│
├── model (entity)
│   ├── User.java
│   └── Document.java
│
├── repository
│   └── DocumentRepository.java
│
├── service
│   ├── DocumentService.java     (Görev 1: @PostAuthorize burada)
│   └── validation
│       └── FileValidator.java   (Görev 2: Magic Bytes/Tika burada)
│
└── SecureVaultApplication.java
```
