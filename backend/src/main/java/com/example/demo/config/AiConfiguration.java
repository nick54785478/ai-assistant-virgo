package com.example.demo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.infra.persistence.RedisChatMemoryRepository;

/**
 * <b>Spring AI 核心配置</b>
 * <p>
 * 負責組裝 LLM 聊天客戶端 (ChatClient) 與記憶體防護機制 (ChatMemory)。
 * </p>
 */
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

	/**
	 * <b>配置對話記憶體 (滑動視窗策略)</b>
	 * <p>
	 * <b>核心防爆機制：</b> {@code maxMessages(20)} 確保系統最多只夾帶最近 20 筆歷史紀錄給 LLM。 這能有效防止
	 * Token 數量隨對話增長而無限制膨脹，避免 Context Window 爆滿或產生高昂運算成本。
	 * </p>
	 * 
	 * @param redisRepo 由 Spring 注入的 Redis 儲存庫實作
	 */
	@Bean
	public ChatMemory chatMemory(RedisChatMemoryRepository redisRepo) {
		return MessageWindowChatMemory.builder()
				// 關鍵：把原本的 InMemoryChatMemoryRepository 換成 Redis
				.chatMemoryRepository(redisRepo).maxMessages(20).build();
	}

	/**
	 * <b>配置 DeepSeek 聊天客戶端 (ChatClient)</b>
	 * <p>
	 * 將 Advisor (記憶體攔截器) 與底層模型參數 (Token 上限) 進行封裝。
	 * </p>
	 */
	@Bean
	public ChatClient deepseekChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
		// 建立記憶體顧問，使其能在每次請求時自動附加歷史上下文
		MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

		return builder.defaultSystem("你是一個全能百科全書...")
				// 使用通用的 ChatOptions，專注解決「斷頭」問題
				.defaultOptions(ChatOptions.builder()
						// 關鍵防禦：徹底解除 Token 長度封印，解決模型話講一半「斷頭」的問題
						.maxTokens(8192).build())
				.defaultAdvisors(advisor).build();
	}

}