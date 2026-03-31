package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * <b>Redis 基礎設施配置</b>
 * <p>
 * 負責設定 RedisTemplate，確保存入 Redis 的物件 (特別是多型的 Spring AI Message) 能夠正確地被序列化與反序列化。
 * </p>
 */
@Configuration
public class RedisConfiguration {

	/**
	 * 自定義 RedisTemplate 以支援 JSON 格式儲存
	 * <p>
	 * 預設的 JdkSerializationRedisSerializer 會產生不可讀的二進位碼， 改用 Jackson 序列化可大幅提升 Redis
	 * 管理工具 (如 RedisInsight) 的可讀性與維護性。
	 * </p>
	 */
	@Bean
	public RedisTemplate<String, Object> chatMemoryRedisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// 1. Key 序列化 (使用 Spring 4.0 推薦寫法)
		template.setKeySerializer(RedisSerializer.string());
		template.setHashKeySerializer(RedisSerializer.string());

		// 2. 配置 Jackson 2 的 ObjectMapper
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		// 啟用多型型別安全驗證 (Polymorphic Type Handling)，確保反序列化時能正確對應子類別
		mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL,
				JsonTypeInfo.As.PROPERTY);

		/*
		 * 3. 序列化器選擇策略 抑制警告說明：雖然 GenericJackson2JsonRedisSerializer 在較新版本標示為 @Deprecated
		 * (為過渡到 Jackson 3)， 但在專案全面升級前，這仍是最穩定且相容性最高的選擇。此處以抑制警告標示我們「有意識地保留」。
		 */
		@SuppressWarnings({ "removal" })
		RedisSerializer<Object> serializer = new GenericJackson2JsonRedisSerializer(mapper);

		template.setValueSerializer(serializer);
		template.setHashValueSerializer(serializer);

		template.afterPropertiesSet();
		return template;
	}
}