package com.example.demo.infra.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * <b>[技術服務] Gemini 雲端大腦對話服務</b>
 * <p>
 * 負責對接 Google Gemini 模型，處理超長文本與高階邏輯推理的混合雲對話。
 * </p>
 */
@Slf4j
@Service
public class GeminiChatService {

	private final ChatClient chatClient;
	private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";

	// 關鍵：明確指定注入剛剛配置的 geminiChatClient
	public GeminiChatService(@Qualifier("geminiChatClient") ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	public Flux<String> streamChat(String message, String chatId) {
		log.info("發起 Gemini 雲端模型對話, Context ID: {}", chatId);

		return chatClient.prompt().user(message).advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
				.stream().chatResponse().map(this::processResponse);
	}

	private String processResponse(ChatResponse response) {
		// 提取內容
		return (response.getResult() != null && response.getResult().getOutput().getText() != null)
				? response.getResult().getOutput().getText()
				: "";
		// 註：若有需要，也可像 Ollama 一樣在此處攔截 Token Usage 統計
	}
}