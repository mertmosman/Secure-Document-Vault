package org.example.securevault.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter; // YENİ NESİL SINIF
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String FILE_PROCESS_QUEUE = "file.process.queue";

    @Bean
    public Queue fileProcessQueue() {
        return new Queue(FILE_PROCESS_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // "2" takısını sildik! Artık yeni Jackson 3 standartı bu.
        return new JacksonJsonMessageConverter();
    }
}