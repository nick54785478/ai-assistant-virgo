package com.example.demo.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.application.port.ChatMemoryManagerPort;
import com.example.demo.application.port.LlmMessageParserPort;
import com.example.demo.application.shared.dto.view.ChatHistoryGottenView;
import com.example.demo.application.shared.dto.view.RawChatMessageGottenView;
import com.example.demo.infra.chat.factory.ChatStrategyFactory;
import com.example.demo.infra.chat.service.ChatbotService;
import com.example.demo.infra.chat.strategy.ChatStreamStrategy;
import com.example.demo.infra.mapper.ChatHistoryMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * [Application Service]
 * 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatApplicationService {

	private final ChatMemoryManagerPort chatMemory;
	private final ChatStrategyFactory strategyFactory;
	private final LlmMessageParserPort llmMessageParser;
	private final ChatHistoryMapper chatHistoryMapper;

	/**
	 * Use Case: 統一處理串流對話
	 * 
	 * @param mode    模式
	 * @param message 訊息
	 * @param chatId  Chat ID
	 */
	public Flux<String> streamChat(String mode, String message, String chatId) {
		log.info("應用層：執行對話任務 [Mode: {}, ChatId: {}]", mode, chatId);
		ChatStreamStrategy strategy = strategyFactory.getStrategy(mode);
		return strategy.streamChat(message, chatId);
	}

	/**
	 * Use Case: 統一處理同步對話 (用於 REST 簡單測試)
	 * 
	 * @param mode    模式
	 * @param message 訊息
	 * @param chatId  Chat ID
	 */
	public String syncChat(String mode, String message, String chatId) {
		// 如果你的策略介面有定義同步方法，就在這裡調用
		// 這裡示範簡單邏輯
		ChatStreamStrategy strategy = strategyFactory.getStrategy(mode);
		if (strategy instanceof ChatbotService generalService) {
			return generalService.chat(message, chatId);
		}
		return "此模式不支援同步對話";
	}

	/**
	 * Use Case: 統一清除對話記憶
	 * 
	 * @param chatId Chat ID
	 */
	public void clearHistory(String chatId) {
		log.info("應用層：清空對話紀錄 [ChatId: {}]", chatId);
		chatMemory.clear(chatId);
	}

	/**
	 * Use Case: 獲取對話歷史紀錄
	 * 
	 * <pre>
	 * 負責編排：組裝 Key -> 讀取記憶 -> 解析模型特定格式 -> 轉換為對外 DTO
	 * </Pre>
	 */
	public List<ChatHistoryGottenView> getChatHistory(String conversationId, String mode) {
		String memoryContextId = conversationId + ":" + mode;

		// 1. 透過 Port 拿回我們自己的 RawChatMessage
		List<RawChatMessageGottenView> messages = chatMemory.getHistory(memoryContextId);

		if (messages == null || messages.isEmpty()) {
			return List.of();
		}

		// 2. 依然是完美的流水線：解析 -> DTO 對應
		return messages.stream().map(llmMessageParser::parse).map(chatHistoryMapper::toDto).toList();
	}

}
