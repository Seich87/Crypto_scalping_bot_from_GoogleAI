package com.example.scalpingBot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bybit")
@Data
public class BybitProperties {
    private Api api = new Api();

    @Data
    public static class Api {
        private String baseUrl;
        private String key;
        private String secret;
        private int recvWindow;
    }
}