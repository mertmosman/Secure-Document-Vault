package org.example.securevault.model;

public enum DocumentStatus {
    PENDING,    // Yüklendi, virüs taraması/işlem bekliyor
    COMPLETED,  // Tarandı ve temiz çıktı
    FAILED      // Zararlı veya hatalı dosya
}