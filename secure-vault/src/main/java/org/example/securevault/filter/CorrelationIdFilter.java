package org.example.securevault.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_LOG_VAR_NAME = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // İstek geldiğinde benzersiz bir ID oluştur (veya frontend göndermişse onu al)
        String correlationId = UUID.randomUUID().toString();

        // Bu ID'yi logların sırt çantasına (MDC) koy
        MDC.put(CORRELATION_ID_LOG_VAR_NAME, correlationId);

        // Cevap dönerken Header'a da ekle ki kullanıcı kendi işlem numarasını bilsin
        response.addHeader("X-Correlation-Id", correlationId);

        try {
            filterChain.doFilter(request, response); // İsteği içeri al
        } finally {
            // İşlem bitince sırt çantasını boşalt (Hafıza sızıntısı olmasın diye çok önemlidir!)
            MDC.remove(CORRELATION_ID_LOG_VAR_NAME);
        }
    }
}