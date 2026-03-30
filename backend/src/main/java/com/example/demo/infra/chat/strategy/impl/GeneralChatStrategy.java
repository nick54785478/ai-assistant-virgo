package com.example.demo.infra.chat.strategy.impl;

import org.springframework.stereotype.Service;

import com.example.demo.infra.chat.service.ChatbotService;
import com.example.demo.infra.chat.strategy.ChatStreamStrategy;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * <b>通用 AI 助手策略</b>
 * <p>
 * 最基礎的對話模式，直接與底層 AI 模型進行互動，不掛載額外的知識庫。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class GeneralChatStrategy implements ChatStreamStrategy {

	private final ChatbotService chatbotService;

	@Override
	public String getSupportedMode() {
		return "GENERAL";
	}

	@Override
	public Flux<String> streamChat(String message, String chatId) {
		return chatbotService.streamChat(message, chatId);
	}
}