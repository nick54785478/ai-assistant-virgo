package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfiguration {

	@Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 註冊 JavaTimeModule 確保它能處理 Java 8 的時間格式 (如 LocalDateTime)
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
