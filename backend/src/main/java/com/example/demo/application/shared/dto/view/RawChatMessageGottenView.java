package com.example.demo.application.shared.dto.view;

/**
 * 原始對話紀錄：用來取代 Spring AI 綁定的 Message 介面，保護應用層不被框架污染。
 */
public record RawChatMessageGottenView(String messageType, // e.g., "user", "assistant", "system"
		String content // 原始文本，可能包含 <think> 等髒標籤
) {
}