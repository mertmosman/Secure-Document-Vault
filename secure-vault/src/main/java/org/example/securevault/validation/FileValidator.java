package org.example.securevault.validation;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class FileValidator {

    private final Tika tika = new Tika();

    // 1. SÖZLÜK: Hangi uzantı, hangi içerik tipiyle eşleşmeli?
    // İleride JPG eklemek istersen sadece buraya bir satır ekleyeceksin.
    private static final Map<String, String> ALLOWED_FORMATS = Map.of(
            "pdf", "application/pdf"
            // "png", "image/png",  <-- İleride bunu açarsan PNG de yüklenir.
            // "jpg", "image/jpeg"
    );

    public void validateFile(MultipartFile file) throws IOException {
        // A. Dosya boş mu?
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Dosya boş olamaz.");
        }

        String fileName = file.getOriginalFilename();
        // Uzantıyı al (örn: "tez.pdf" -> "pdf")
        String extension = getExtension(fileName);

        // B. Uzantı bizim listemizde var mı?
        if (!ALLOWED_FORMATS.containsKey(extension)) {
            throw new RuntimeException("Desteklenmeyen dosya uzantısı: ." + extension);
        }

        // C. Tika ile GERÇEK içeriği (Magic Bytes) oku [cite: 39, 40]
        String detectedMimeType = tika.detect(file.getInputStream());

        // D. KRİTİK NOKTA: Uzantının vaat ettiği tip ile gerçek tip tutuyor mu?
        String expectedMimeType = ALLOWED_FORMATS.get(extension);

        if (!expectedMimeType.equals(detectedMimeType)) {
            // Örnek: Uzantı .pdf ama içerik image/jpeg (Hacker yakalandı)
            throw new RuntimeException(
                    String.format("Dosya sahte! Uzantı (.%s) ile içerik (%s) uyuşmuyor.",
                            extension, detectedMimeType)
            );
        }
    }

    // Yardımcı Metot: Dosya isminden uzantıyı ayıklar
    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new RuntimeException("Geçersiz dosya ismi.");
        }
        // Son noktadan sonrasını al ve küçült (PDF -> pdf)
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    // YENİ: InputStream üzerinden Tika güvenlik taraması
    public void validateStream(java.io.InputStream inputStream, String fileName) throws Exception {
        org.apache.tika.Tika tika = new org.apache.tika.Tika();
        String detectedType = tika.detect(inputStream); // Magic Byte kontrolü

        if (!detectedType.equals("application/pdf")) {
            throw new Exception("Güvenlik İhlali: " + fileName + " gerçek bir PDF değil! (Tespit edilen: " + detectedType + ")");
        }
    }
}
