package org.example.securevault.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    public MinioStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    // 1. Dosyayı MinIO'ya Yükler
    public String uploadFile(MultipartFile file) throws Exception {
        // Kova (Bucket) var mı kontrol et, yoksa oluştur
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // Aynı isimde dosyalar çakışmasın diye UUID ile benzersiz isim oluşturuyoruz
        String originalFilename = file.getOriginalFilename();
        String objectKey = UUID.randomUUID().toString() + "_" + originalFilename;

        // Dosyayı MinIO'ya gönder (Memory'yi şişirmemek için Stream kullanıyoruz)
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }

        return objectKey; // Veritabanına kaydedeceğimiz adres bu!
    }

    // 2. Dosyayı MinIO'dan İndirir
    public InputStream downloadFile(String objectKey) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .build()
        );
    }
    // 3. Dosyayı MinIO'dan Siler
    public void deleteFile(String objectKey) throws Exception {
        minioClient.removeObject(
                io.minio.RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .build()
        );}
}