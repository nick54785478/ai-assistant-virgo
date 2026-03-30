package com.example.demo.infra.chat.strategy.impl;

import org.springframework.stereotype.Service;

import com.example.demo.infra.chat.service.ChatRagService;
import com.example.demo.infra.chat.strategy.ChatStreamStrategy;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * <b>DDD 領域專家策略</b>
 * <p>
 * 專門處理具備 RAG (檢索增強生成) 能力的對話。 它會先去 VectorStore 撈取技術規範，再交給 LLM 進行回答。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class DddExpertChatStrategy implements ChatStreamStrategy {

	private final ChatRagService chatRagService;

	@Override
	public String getSupportedMode() {
		return "EXPERT_DDD";
	}

	@Override
	public Flux<String> streamChat(String message, String chatId) {
		return chatRagService.streamChatWithDddExpert(chatId, message);
	}
}