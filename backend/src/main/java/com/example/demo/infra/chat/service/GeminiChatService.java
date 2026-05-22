package com.example.demo.infra.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.example.demo.application.port.ChatMemoryManagerPort;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * <b>[Infrastructure Service] Gemini 雲端大腦對話服務 (策略實作)</b>
 * 
 * <pre>
 * 本類別為 Virgo 混合雲雙腦架構中的「雲端大腦」具體實作，負責對接 Google Gemini 基礎模型 (如 gemini-2.0-flash)。
 * 專門處理需要超大上下文視窗 (Context Window) 與高階邏輯推理的複雜任務。 
 * 透過 Spring AI 與 Project Reactor 的整合，提供非阻塞、低延遲的 Reactive 串流輸出。
 * </pre>
 */
@Slf4j
@Service
public class GeminiChatService {

	private final ChatClient chatClient;

	// 定義 Spring AI 預設的記憶體 Advisor 參數鍵值，用於注入對話上下文 ID
	private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";

	/**
	 * <b>建構子注入與 Bean 隔離</b>
	 * 
	 * <pre>
	 * 關鍵防禦：在雙腦架構下，Spring 容器中同時存在 Ollama 與 Gemini 的 ChatClient。
	 * 必須使用 {@link Qualifier} 明確指定注入名為 "geminiChatClient" 的實例，避免依賴衝突。
	 * </pre>
	 * 
	 * @param chatClient 專屬於 Gemini 模型的 ChatClient 實例
	 */
	public GeminiChatService(@Qualifier("geminiChatClient") ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * <b>發起非阻塞式 (Reactive) 雲端模型對話串流</b>
	 * 
	 * <pre>
	 * 透過 Spring AI 的 Advisor 機制，將複合鍵值 (chatId) 動態綁定至請求中，
	 * 藉此喚醒底層 {@link ChatMemoryManagerPort} 進行記憶體的物理隔離與歷史回溯。
	 * </pre>
	 * 
	 * @param message 使用者輸入的最新對話內容
	 * @param chatId  複合式記憶鍵值 (格式: "conversationId:mode")，確保不同模式的記憶不互串
	 * @return 包含 AI 碎塊化回應的 Flux 響應式資料流 (Token Stream)
	 */
	public Flux<String> streamChat(String message, String chatId) {
		log.info("發起 Gemini 雲端模型對話, Context ID: [{}]", chatId);

		return chatClient.prompt().user(message)
				// 注入記憶體攔截器，確保 AI 具備先前的對話脈絡
				.advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)).stream().chatResponse()
				.map(this::processResponse);
	}

	/**
	 * <b>解析並淨化 AI 回應流 (防腐層微處理)</b>
	 * 
	 * <pre>
	 * 從 Spring AI 封裝的 {@link ChatResponse} 中安全地提取文本輸出。 具備 Null-Safe
	 * 防禦機制，避免串流傳輸過程中因空封包導致的 NullPointerException。
	 * </pre>
	 * 
	 * @param response Spring AI 原始的對話回應物件
	 * @return 提取出的純文本片段；若無內容則回傳空字串以維持 Flux 流暢度
	 */
	private String processResponse(ChatResponse response) {
		// 安全提取：逐層檢查 Null，確保串流的絕對穩定性
		return (response.getResult() != null && response.getResult().getOutput().getText() != null)
				? response.getResult().getOutput().getText()
				: "";

		/*
		 * 架構擴充點： 若未來 Gemini API 開放更詳細的串流 Token Usage 數據， 可在此處攔截 response.getMetadata()
		 * 並發送 Event 給計費/統計微服務。
		 */
	}
}