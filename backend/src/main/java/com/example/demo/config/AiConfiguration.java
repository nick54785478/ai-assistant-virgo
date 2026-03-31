package com.example.demo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
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
	@Bean("deepseekChatClient")
	public ChatClient deepseekChatClient(@Qualifier("ollamaChatModel") ChatModel ollamaModel, ChatMemory chatMemory) {

		MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

		// 自己用 ollamaModel 產生專屬的 Builder
		return ChatClient.builder(ollamaModel).defaultSystem("你是一個全能百科全書...")
				.defaultOptions(ChatOptions.builder().maxTokens(8192) // 解除 Token 封印
						.build())
				.defaultAdvisors(advisor) // 配置記憶紀錄器
				.build();
	}

	/**
	 * <b>配置 Gemini 雲端大腦 (Google Gen AI)</b> 🚀 解法：透過 @Qualifier 精準注入
	 * googleGenAiChatModel
	 */
	@Bean("geminiChatClient")
	public ChatClient geminiChatClient(@Qualifier("googleGenAiChatModel") ChatModel geminiModel,
			ChatMemory chatMemory) {

		// 產生記憶體攔截器
		MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

		// 自己用 geminiModel 產生專屬的 Builder
		return ChatClient.builder(geminiModel)
				.defaultSystem("你是一位具備頂級邏輯推理能力的雲端架構大腦，名為 Gemini。請以專業、精準且富有同理心的語氣協助使用者解決最複雜的問題。")
				.defaultAdvisors(advisor).build();
	}

}