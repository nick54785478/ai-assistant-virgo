package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <b>全域 Jackson 序列化配置</b>
 * <p>
 * 定義系統預設的 JSON 處理規則。 尤其對於 DDD 實體中常用的 Java 8 時間 API (如 LocalDateTime) 提供必要支援。
 * </p>
 */
@Configuration
public class JacksonConfiguration {

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		// 註冊 JavaTimeModule 確保 Controller 與前端互動時，時間格式不會出錯
		mapper.registerModule(new JavaTimeModule());
		return mapper;
	}
}
