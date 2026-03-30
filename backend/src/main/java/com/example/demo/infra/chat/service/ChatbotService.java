package com.example.demo.infra.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * <b>[技術服務] 通用 AI 助手對話服務</b>
 * 
 * <pre>
 * 職責：負責處理非 RAG 模式下的標準 AI 對話。 
 * 核心特性： 
 * 1. 支援同步 (Blocking) 與串流 (Streaming) 兩種對話模式。
 * 2. 自動掛載 ChatMemory Advisor，確保對話具備上下文記憶能力。 
 * 3. 攔截底層 AI 模型 (如 Ollama/DeepSeek) 的 Metadata，即時回傳 Token 消耗統計。
 * </pre>
 *
 */
@Slf4j
@Service
public class ChatbotService {

	/**
	 * 核心 AI 客戶端，封裝了與 LLM 互動的底層邏輯
	 */
	private final ChatClient chatClient;

	/**
	 * Spring AI 規範中用於傳遞對話 ID 的 Key 標記
	 */
	private final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";

	/**
	 * 建構子注入
	 * 
	 * @param chatClient 指定注入在 AiConfiguration 中配置的 deepseekChatClient Bean
	 */
	public ChatbotService(@Qualifier("deepseekChatClient") ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * <b>同步對話模式</b>
	 * <p>
	 * 適用於對延遲要求不高，或需要一次性獲取完整回應的場景（如單元測試或內部 REST 呼叫）。
	 * </p>
	 * 
	 * @param message 使用者輸入文字
	 * @param chatId  對話上下文 ID，對應 Redis 中的存儲鍵
	 * @return AI 生成的完整文字內容
	 */
	public String chat(String message, String chatId) {
		return chatClient.prompt().user(message)
				// 🚀 關鍵：將 chatId 注入 Advisor，使 AI 能提取歷史紀錄
				.advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)).call().content();
	}

	/**
	 * <b>核心串流對話方法 (通用模式)</b>
	 * <p>
	 * 採用 Reactive 程式設計風格，適合 WebSocket 即時回傳場景。 透過監聽 ChatResponse 串流，實現在最後一個 Chunk 附帶
	 * Token 統計註腳。
	 * </p>
	 * 
	 * @param message 使用者輸入文字
	 * @param chatId  對話上下文 ID
	 * @return 包含內容與統計註腳的字串流
	 */
	public Flux<String> streamChat(String message, String chatId) {
		log.info("發起通用 AI 對話, Context ID: {}", chatId);

		return chatClient.prompt().user(message).advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
				.stream()
				/*
				 * 關鍵改變： 我們不直接拿 .content() (這會只給 String)， 改拿 .chatResponse() 以便獲取包含
				 * Usage、Metadata 的完整回應物件。
				 */
				.chatResponse().map(this::processResponseWithUsage);
	}

	/**
	 * <b>內部共用方法：解析響應與 Token 結算</b>
	 * <p>
	 * 邏輯： 1. 正常提取當前資料區塊 (Chunk) 的文字內容。 2. 檢查 Metadata 中是否包含 Usage 數據 (通常由 Ollama
	 * 在最後一個 Chunk 給出)。 3. 若存在數據，則將 Markdown 格式的統計資訊附加在文字末尾。
	 * </p>
	 * 
	 * @param response Spring AI 封裝的響應物件
	 * @return 格式化後的文字內容
	 */
	private String processResponseWithUsage(ChatResponse response) {
		// 1. 安全提取文字內容 (避開 NullPointerException)
		String content = (response.getResult() != null && response.getResult().getOutput().getText() != null)
				? response.getResult().getOutput().getText()
				: "";

		// 2. 攔截統計數據：僅當總消耗大於 0 時才進行 Markdown 拼接
		if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
			Usage usage = response.getMetadata().getUsage();
			Integer total = usage.getTotalTokens();

			if (total != null && total > 0) {
				// 產生易於閱讀的統計註腳 (Markdown 格式)
				content += String.format("\n\n---\n*📊 Ollama 統計：總消耗 **%d** (輸入: %d / 輸出: %d)*", total,
						usage.getPromptTokens(), usage.getCompletionTokens());
			}
		}
		return content;
	}
}