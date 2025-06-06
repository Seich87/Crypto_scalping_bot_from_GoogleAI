package com.example.scalpingBot.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация для HTTP-клиентов, используемых в приложении.
 */
@Configuration
public class ApiClientConfig {

    /**
     * Создает и настраивает бин RestTemplate, который будет использоваться для
     * отправки синхронных HTTP-запросов к API бирж.
     *
     * @param builder RestTemplateBuilder, автоматически сконфигурированный Spring Boot.
     * @return настроенный экземпляр RestTemplate.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {

        // Создаем фабрику для HTTP-запросов
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        // Устанавливаем таймаут на установку соединения в миллисекундах (5 секунд)
        requestFactory.setConnectTimeout(5000);

        // Устанавливаем таймаут на чтение ответа в миллисекундах (10 секунд)
        requestFactory.setReadTimeout(10000);

        // Передаем сконфигурированную фабрику в RestTemplateBuilder
        return builder
                .requestFactory(() -> requestFactory)
                .build();
    }
}