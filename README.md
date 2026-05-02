# 🛡️ Secure Document Vault (Güvenli Belge Kasası)

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.1-green)
![Security](https://img.shields.io/badge/Spring_Security-6-red)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)
![Architecture](https://img.shields.io/badge/Architecture-Microservices_Ready-purple)

**Secure Document Vault**, yüksek güvenlik standartlarına (OWASP) uygun olarak geliştirilmiş, siber saldırılara karşı güçlendirilmiş ve yüksek trafik (High Load) altında çalışabilen dağıtık bir dosya depolama ve yönetim REST API'sidir.

Bu proje; **IDOR, DDoS, Brute-Force** ve **Malicious File Upload** gibi yaygın web saldırılarına karşı savunma yapmanın yanı sıra, **Asenkron Mesajlaşma**, **Dağıtık Ön Bellekleme (Distributed Caching)** ve **S3 Uyumlu Nesne Depolama** mimarilerinin Java Spring Boot ile nasıl entegre edileceğini göstermek amacıyla tasarlanmıştır.

---

## 🚀 Özellikler ve Güvenlik Önlemleri

Bu proje, "Security First" (Öncelikle Güvenlik) ve "High Performance" (Yüksek Performans) yaklaşımlarıyla geliştirilmiştir:

### 🔐 1. Kusursuz Kimlik ve Erişim Yönetimi (IAM)
* **Çift Jetonlu (Dual-Token) Auth:** Sadece Access Token değil, güvenli oturum yönetimi için `RefreshToken` sistemi entegre edilmiştir.
* **RBAC (Rol Tabanlı Erişim):** `ADMIN` ve `USER` rolleri ile API bazlı yetkilendirme.
* **Password Hashing:** Şifreler veritabanında asla açık tutulmaz (BCrypt kullanılarak hashlenir).

### 🛡️ 2. Gelişmiş Uygulama Güvenliği
* **IDOR Koruması (Broken Access Control):** Kullanıcıların URL üzerinden ID değiştirip başkasına ait dosyaları görmesi (Yetki Yükseltme) engellenmiştir.
* **Güvenli Dosya Yükleme (Magic Bytes):** Sadece dosya uzantısına (.pdf) bakılmaz, dosyanın `hex signature` (büyülü baytları) analiz edilerek sahte dosyalar engellenir.
* **Dağıtık Hız Sınırlandırma (Distributed Rate Limiting - Redis):** Brute-force ve DDoS saldırılarına karşı her kullanıcıya IP/Username bazlı kota uygulanır. Sistem çoklu sunucuda çalışsa bile limitler Redis üzerinden senkronize edilir.

### ⚡ 3. Kurumsal Mimari (Enterprise Architecture)
* **Signed URL ile Doğrudan İndirme (Offloading):** Kullanıcılar dosyaları Spring Boot üzerinden değil, MinIO üzerinden üretilen "5 Dakika Geçerli İmzalı Linkler (Pre-Signed URLs)" ile indirir. Backend sunucusunun RAM ve bant genişliği darboğazları (bottleneck) tamamen ortadan kaldırılmıştır.
* **Asenkron İşlem (RabbitMQ):** Dosya yüklendiğinde, analiz veya bildirim gibi ağır işlemler ana HTTP thread'ini meşgul etmemesi için kuyruğa atılır.

---

## 📐 Sistem Tasarımı ve Mimari (System Design)

### Neden Bu Teknolojiler? (Trade-offs)
* **Neden PostgreSQL yerine MinIO (S3)?**
  * *Sorun:* Büyük boyutlu dosyaları (BLOB) ilişkisel veritabanında saklamak; veritabanını şişirir, yedekleme (backup) sürelerini uzatır ve veritabanı RAM'ini gereksiz işgal eder.
  * *Çözüm:* S3 uyumlu bir nesne depolama sunucusu olan MinIO kullanıldı. Veritabanı sadece dosyanın "metadatasını" (ID, isim, sahip), MinIO ise fiziksel byte'ları tutar. Signed URL mimarisi sadece S3 tabanlı sistemlerde mümkündür.
* **Neden RAM Tabanlı Rate Limit (Bucket4j) yerine Redis?**
  * *Sorun:* Local RAM tabanlı hız sınırları, uygulama 3 farklı Docker konteynerine (Horizontal Scaling) bölündüğünde işlevsiz kalır. Kullanıcı bir sunucuda limitini doldursa bile diğerinden istek atmaya devam edebilir.
  * *Çözüm:* Tüm sunucuların ortak eriştiği, mikro saniyeler içinde cevap veren in-memory (RAM) tabanlı Redis veritabanı kullanılarak hız limitleri merkezileştirilmiştir.

### Mimari Akış Diyagramı (Architecture Diagram)
##
<img width="524" height="360" alt="image" src="https://github.com/user-attachments/assets/649dfb27-5a53-4284-b71c-ac2033c03af3" />

---

## 📊 Performans ve Yük Testi Raporu (K6 Load Test)

Sistemin yüksek trafik altındaki dayanıklılığı **k6** kullanılarak test edilmiştir.
* **Senaryo:** 50 saniye boyunca saniyede 100 eşzamanlı sanal kullanıcı (100 VUs) ile `/api/auth/login` ucuna taarruz.
* **Optimizasyonlar:** 
  * N+1 veritabanı sorgu problemi çözüldü (Login başına 4 SELECT sorgusu 1'e düşürüldü).
  * HikariCP connection pool limiti 10'dan 50'ye çıkarıldı.
  * Tomcat max-threads limiti 400'e çekildi.

**Test Sonuçları:**
* Toplam İşlenen İstek: **2428**
* Başarılı İsteklerin Ortalama Dönüş Süresi (Latency): **~4.85 ms** (Mükemmel)
* Başarısız İstek Oranı (Dropped): **%24.50**

**Darboğaz (Bottleneck) Analizi:**
I/O darboğazları çözülmesine rağmen alınan %24'lük HTTP 500/Timeout hataları incelendiğinde, sorunun veritabanı değil **CPU satürasyonu** olduğu tespit edilmiştir. Spring Security tarafından kullanılan **BCrypt** şifreleme algoritması, Brute-Force saldırılarını engellemek amacıyla *kasıtlı olarak* CPU'yu yoracak şekilde tasarlanmıştır. Saniyede 100 BCrypt işlemi yerel makinenin CPU kaynaklarını tüketmiştir. Bu bir "hata" değil, beklenen bir güvenlik önlemidir. Canlı ortamda bu darboğaz yatay ölçekleme (Horizontal Scaling) ile aşılabilir.

---

## 🛠️ Teknoloji Yığını (Tech Stack)

* **Backend:** Java 17, Spring Boot 4.0.1
* **Veritabanı:** PostgreSQL 15 (Metadata & Yetkilendirme)
* **Object Storage:** MinIO (S3 Compatible - Fiziksel Dosyalar)
* **In-Memory Cache / Rate Limit:** Redis 7
* **Message Broker:** RabbitMQ 3
* **Güvenlik:** Spring Security 6, JJWT (0.11.5)
* **Dosya Analizi:** Apache Tika Core
* **Yük Testi:** K6
* **DevOps:** Docker, Docker Compose

---

## ⚙️ Kurulum ve Çalıştırma (Docker Konteynerizasyonu)

Uygulama tam teşekküllü bir mikroservis altyapısına sahip olduğu için en sağlıklı çalışma yöntemi **Docker Compose**'dur.

1. Bilgisayarınızda Docker Desktop'ın açık olduğundan emin olun.
2. Repoyu klonlayıp kök dizine gidin:
   ```bash
   git clone [https://github.com/mertmosman/secure-vault.git](https://github.com/mertmosman/secure-vault.git)
   cd secure-vault
   
```
3. Tek komutla tüm orduyu (Spring Boot, Postgres, Redis, RabbitMQ, MinIO) ayağa kaldırın:
   ```bash
   docker compose up --build -d
   ```
4. Uygulama **http://localhost:8081** adresinde çalışmaya başlayacaktır.

---

## 📖 API Dokümantasyonu (Swagger)

Uygulama çalıştıktan sonra, tüm endpoint'leri görmek ve test etmek için tarayıcınızdan şu adrese gidin:

👉 **http://localhost:8081/swagger-ui/index.html**

**Temel Endpointler:**
* `POST /api/auth/register` - Kayıt Olma
* `POST /api/auth/login` - Giriş Yap (Access + Refresh Token Döner)
* `POST /api/auth/refresh` - Süresi dolan token'ı yenile
* `POST /api/documents/upload` - Belge Yükle (Rate Limiting Korumalı 🔒)
* `GET /api/documents/{id}/download` - MinIO İmzalı İndirme Linki Üretir 🔒
* `GET /api/users` - Kullanıcıları Listele (Sadece Admin 🔒)

---

## 📂 Dosya Hiyerarşisi
```text
secure-vault/
├── .mvn/ wrapper/
├── src/
│   ├── main/
│   │   ├── java/org/example/securevault/
│   │   │   ├── config/             # Güvenlik, Swagger ve Aspect Ayarları
│   │   │   ├── controller/         # REST API Uç Noktaları
│   │   │   ├── dto/                # Veri Transfer Objeleri
│   │   │   ├── exception/          # Global Hata Yönetimi
│   │   │   ├── model/              # User, Document, AuditLog, RefreshToken
│   │   │   ├── repository/         # Spring Data JPA Arayüzleri
│   │   │   ├── service/            # İş Mantığı (MinIO, Redis, JWT Servisleri)
│   │   │   └── validation/         # Magic Bytes (Tika) Kontrolü
│   │   └── resources/
│   │       └── application.properties
│   └── test/                       # JUnit ve Mockito Birim Testleri
│       └── load-test.js            # K6 Stres Testi Senaryosu
├── docker-compose.yml              # Tüm servislerin orkestrasyonu
├── Dockerfile                      # Spring Boot için imaj oluşturucu
├── pom.xml                         # Kütüphane Bağımlılıkları
└── README.md                       # Mimari Dokümantasyon
```
