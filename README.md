# 🛡️ Secure Document Vault (Güvenli Belge Kasası)

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.1-green)
![Security](https://img.shields.io/badge/Spring_Security-6-red)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

**Secure Document Vault**, yüksek güvenlik standartlarına (OWASP) uygun olarak geliştirilmiş, siber saldırılara karşı güçlendirilmiş bir dosya depolama ve yönetim REST API'sidir.

Bu proje, sadece dosya yüklemeyi değil; **IDOR, DDoS, Brute-Force** ve **Malicious File Upload** gibi yaygın web saldırılarına karşı nasıl savunma yapılacağını göstermek amacıyla tasarlanmıştır.

---

## 🚀 Özellikler ve Güvenlik Önlemleri

Bu proje, "Security First" (Önce Güvenlik) yaklaşımıyla geliştirilmiştir:

### 🔐 1. Kimlik ve Erişim Yönetimi (IAM)
* **JWT (JSON Web Token):** Stateless (durumsuz) kimlik doğrulama.
* **RBAC (Rol Tabanlı Erişim):** `ADMIN` ve `USER` rolleri ile yetkilendirme.
* **Password Hashing:** Şifreler veritabanında asla açık tutulmaz (BCrypt/Argon2 kullanımı için altyapı).

### 🛡️ 2. Uygulama Güvenliği
* **IDOR Koruması (Broken Access Control):** Spring Security `@PostAuthorize` kullanılarak, kullanıcıların URL üzerinden ID değiştirip başkasına ait dosyaları görmesi engellenmiştir.
* **Güvenli Dosya Yükleme:**
    * **Magic Bytes Kontrolü:** Sadece dosya uzantısına (.pdf) bakılmaz, dosyanın `hex signature` (büyülü baytları) analiz edilerek içeriğin gerçekten PDF olup olmadığı (Apache Tika / Java IO ile) doğrulanır.
* **Rate Limiting (Hız Sınırlama):** **Bucket4j** kullanılarak Token Bucket algoritması uygulanmıştır. Spam ve DDoS saldırılarına karşı her kullanıcıya belirli bir kota (örn: dakikada 10 istek) tanımlanmıştır.

### 👁️ 3. Gözlemlenebilirlik (Auditing)
* **AOP (Aspect Oriented Programming):** Sisteme entegre edilen "Gizli Ajan" (AuditLoggingAspect), kritik işlemleri (Dosya Yükleme, Silme) çalışma anında yakalar.
* **Denetim İzi:** Kimin, hangi IP adresinden, ne zaman, hangi işlemi yaptığı veritabanındaki `audit_logs` tablosuna kaydedilir.

---

## 🛠️ Teknoloji Yığını (Tech Stack)

* **Backend:** Java 17, Spring Boot 4.0.1
* **Veritabanı:** PostgreSQL 15
* **Güvenlik:** Spring Security 6, JJWT (0.11.5)
* **Rate Limiting:** Bucket4j
* **Dosya Analizi:** Apache Tika Core
* **API Dokümantasyonu:** SpringDoc OpenAPI (Swagger UI)
* **DevOps:** Docker, Docker Compose
* **Test:** JUnit 5, Mockito, Postman

---

## ⚙️ Kurulum ve Çalıştırma

Projeyi çalıştırmak için iki yöntem vardır. En kolayı **Docker** kullanmaktır.

### Yöntem 1: Docker ile (Önerilen) 🐳
Bilgisayarınızda Docker ve Docker Compose yüklü olmalıdır.

1.  Repoyu klonlayın:
    ```bash
    git clone [https://github.com/mertmosman/secure-vault.git](https://github.com/mertmosman/secure-vault.git)
    cd secure-vault
    ```

2.  Projeyi paketleyin ve konteynerleri ayağa kaldırın:
    ```bash
    # Önce Maven ile build alın (Testleri atlayarak hızlı build)
    ./mvnw clean package -DskipTests

    # Docker Compose ile başlatın
    docker-compose up --build
    ```

3.  Uygulama **http://localhost:8081** adresinde çalışmaya başlayacaktır.

### Yöntem 2: Lokal Kurulum (Manuel)
1.  Bilgisayarınızda **PostgreSQL** kurulu olmalı ve `securevault_db` adında bir veritabanı oluşturulmalıdır.
2.  `src/main/resources/application.properties` dosyasındaki veritabanı ayarlarını kendi lokal ayarlarınıza göre güncelleyin.
3.  Uygulamayı çalıştırın:
    ```bash
    ./mvnw spring-boot:run
    ```

---

## 📖 API Dokümantasyonu (Swagger)

Uygulama çalıştıktan sonra, tüm endpoint'leri görmek ve test etmek için tarayıcınızdan şu adrese gidin:

👉 **http://localhost:8081/swagger-ui/index.html**
<img width="914" height="927" alt="image" src="https://github.com/user-attachments/assets/a0b05a0d-2f0f-4c81-81a3-16627a0180f4" />

**Temel Endpointler:**
* `POST /api/auth/register` - Kayıt Ol
* `POST /api/auth/login` - Giriş Yap (Token Al)
* `POST /api/documents/upload` - Belge Yükle (Token Gerekli 🔒)
* `GET /api/documents/{id}` - Belge Görüntüle (Sadece Sahibi Görebilir 🔒)
* `GET /api/users` - Kullanıcıları Listele (Sadece Admin 🔒)

---
```
secure-vault/
├── .mvn/ wrapper/                  # (Maven Wrapper dosyaları)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/example/securevault/
│   │   │       │
│   │   │       ├── config/                      # ⚙️ KONFİGÜRASYON
│   │   │       │   ├── AuditLoggingAspect.java  # (AOP - Gizli Ajan)
│   │   │       │   ├── DataInitializer.java     # (Başlangıç verileri)
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   ├── OpenApiConfig.java       # (Swagger Ayarları)
│   │   │       │   └── SecurityConfig.java      # (Ana Güvenlik Ayarı)
│   │   │       │
│   │   │       ├── controller/                  # 🎮 API UÇ NOKTALARI
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── DocumentController.java
│   │   │       │   └── UserController.java
│   │   │       │
│   │   │       ├── dto/                         # 📦 VERİ TRANSFER OBJELERİ
│   │   │       │   ├── AuthResponse.java
│   │   │       │   ├── LoginRequest.java
│   │   │       │   └── RegisterRequest.java
│   │   │       │
│   │   │       ├── exception/                   # 🚨 HATA YÖNETİMİ (YENİ EKLENDİ)
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       │
│   │   │       ├── model/                       # 🗄️ VERİTABANI VARLIKLARI
│   │   │       │   ├── AuditLog.java
│   │   │       │   ├── Document.java
│   │   │       │   └── User.java
│   │   │       │
│   │   │       ├── repository/                  # 💾 VERİ ERİŞİM KATMANI
│   │   │       │   ├── AuditLogRepository.java
│   │   │       │   ├── DocumentRepository.java
│   │   │       │   └── UserRepository.java
│   │   │       │
│   │   │       ├── service/                     # 🧠 İŞ MANTIĞI
│   │   │       │   ├── CustomUserDetailsService.java
│   │   │       │   ├── DocumentService.java
│   │   │       │   ├── JwtService.java
│   │   │       │   ├── RateLimitingService.java
│   │   │       │   └── UserService.java
│   │   │       │
│   │   │       ├── validation/                  # ✅ DOĞRULAMA
│   │   │       │   └── FileValidator.java       # (Magic Bytes/Tika kontrolü)
│   │   │       │
│   │   │       └── SecureVaultApplication.java  # 🚀 BAŞLATICI
│   │   │
│   │   └── resources/
│   │       └── application.properties           # 🔧 AYAR DOSYASI
│   │
│   └── test/
│       └── java/
│           └── org/example/securevault/
│               ├── service/
│               │   └── JwtServiceTest.java      # 🧪 BİRİM TESTİ
│               └── SecureVaultApplicationTests.java
│
├── target/                     # (Derleme çıktıları - dokunma)
├── uploads/                    # (Lokal test için dosya yükleme alanı - opsiyonel)
├── .gitignore                  # (Git ayar dosyası)
├── docker-compose.yml          # 🐳 DOCKER ORKESTRA (Ana dizinde olmalı!)
├── Dockerfile                  # 🐳 DOCKER İMAJ (Ana dizinde olmalı!)
├── mvnw                        # (Maven çalıştırıcı)
├── mvnw.cmd                    # (Maven çalıştırıcı - Windows)
├── pom.xml                     # 📋 KÜTÜPHANELER
└── README.md                   # 📖 PROJE DOKÜMANTASYONU
```
## 🧪 Test Süreci

### Unit Testler
Servis katmanının (özellikle JWT ve yetkilendirme mantığının) doğruluğu JUnit testleri ile güvence altına alınmıştır.
```bash
./mvnw test
