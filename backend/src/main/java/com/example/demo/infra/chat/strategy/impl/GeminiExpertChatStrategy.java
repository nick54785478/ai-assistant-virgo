package com.example.demo.infra.chat.strategy.impl;

import org.springframework.stereotype.Service;

import com.example.demo.infra.chat.service.GeminiChatService;
import com.example.demo.infra.chat.strategy.ChatStreamStrategy;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * <b>[維運實作] 雲端大腦策略 (Gemini)</b>
 * <p>
 * 當前端傳入 EXPERT_GEMINI 模式時，會自動路由至此策略進行雲端推理。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class GeminiExpertChatStrategy implements ChatStreamStrategy {

	private final GeminiChatService geminiChatService;

	@Override
	public String getSupportedMode() {
		return "EXPERT_GEMINI"; // 對應前端的 mode 字串
	}

	@Override
	public Flux<String> streamChat(String message, String chatId) {
		return geminiChatService.streamChat(message, chatId);
	}
}