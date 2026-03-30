package com.example.demo.infra.adapter;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.ChatMemoryManagerPort;
import com.example.demo.application.shared.dto.view.RawChatMessageGottenView;

import lombok.RequiredArgsConstructor;

/**
 * 將 Application 層的介面，轉譯為 Spring AI 的 ChatMemory 操作
 */
@Component
@RequiredArgsConstructor
class ChatMemoryManagerAdapter implements ChatMemoryManagerPort {

	private final ChatMemory chatMemory; // 這裡才是真正依賴 Spring AI 的地方

	@Override
	public List<RawChatMessageGottenView> getHistory(String contextId) {
		List<Message> messages = chatMemory.get(contextId);
		if (messages == null || messages.isEmpty()) {
			return List.of();
		}

		// 將 Spring AI 的 Message 轉換為乾淨的 RawChatMessage 送回給 Application 層
		return messages.stream().map(
				msg -> new RawChatMessageGottenView(msg.getMessageType().getValue(), msg.getText() != null ? msg.getText() : ""))
				.toList();
	}

	@Override
	public void clear(String contextId) {
		chatMemory.clear(contextId);
	}
}