package com.example.scalpingBot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "binance")
@Data
public class BinanceProperties {
    private Api api = new Api();
    private Ws ws = new Ws();

    @Data
    public static class Api {
        private String baseUrl;
        private String key;
        private String secret;
    }

    @Data
    public static class Ws {
        private String baseUrl;
    }
}