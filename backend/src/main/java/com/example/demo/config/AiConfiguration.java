package com.example.demo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.infra.persistence.RedisChatMemoryRepository;

@Configuration
public class AiConfiguration {

//	// 1. 使用 2.0 最新版的滑動視窗記憶體
//	@Bean
//	public ChatMemory chatMemory() {
//		return MessageWindowChatMemory.builder()
//				// 指定儲存庫為記憶體
//				.chatMemoryRepository(new InMemoryChatMemoryRepository())
//				// 核心防爆機制：最多只記住最近的 20 筆對話
//				.maxMessages(20).build();
//	}

	@Bean
	public ChatMemory chatMemory(RedisChatMemoryRepository redisRepo) {
		return MessageWindowChatMemory.builder()
				// 關鍵：把原本的 InMemoryChatMemoryRepository 換成 Redis
				.chatMemoryRepository(redisRepo).maxMessages(20).build();
	}

	@Bean
	public ChatClient deepseekChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
		MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
		
		return builder.defaultSystem("你是一個全能百科全書...")
				// 使用通用的 ChatOptions，專注解決「斷頭」問題
				.defaultOptions(ChatOptions.builder()
						.maxTokens(8192) // 徹底解除 Token 長度封印
						.build())
				.defaultAdvisors(advisor)
				.build();
	}

}