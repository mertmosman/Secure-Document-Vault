# OpenJDK yerine daha kararli olan Eclipse Temurin kullaniyoruz
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY target/secure-vault-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
