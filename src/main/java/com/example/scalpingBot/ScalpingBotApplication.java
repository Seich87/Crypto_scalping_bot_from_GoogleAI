package com.example.scalpingBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // <-- Добавить импорт

@SpringBootApplication
@EnableScheduling // <-- Добавить эту аннотацию
public class ScalpingBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScalpingBotApplication.class, args);
	}

}