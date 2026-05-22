package com.example.demo.infra.adapter;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.ChatMemoryManagerPort;
import com.example.demo.application.shared.dto.view.RawChatMessageGottenView;

import lombok.RequiredArgsConstructor;

/**
 * <b>[Infrastructure Adapter] 對話記憶體管理實作 (防腐層)</b>
 * 
 * <pre>
 *   本類別為 ChatMemoryManagerPort 的具體實作，位於六角架構的基礎設施層 (Infrastructure Layer)。
 * 其核心職責是作為防腐層 (Anti-Corruption Layer, ACL)，將底層 Spring AI 框架專屬的 {@link ChatMemory} 操作細節隱藏起來，
 * 並將 AI 回傳的 {@link Message} 轉換為應用層 (Application Layer) 所需的純淨視圖 {@link RawChatMessageGottenView}，
 * 從而確保核心業務邏輯不會被外部 AI 框架的改版所污染。
 * </pre>
 */
@Component
@RequiredArgsConstructor
class ChatMemoryManagerAdapter implements ChatMemoryManagerPort {

	// 這裡才是真正依賴 Spring AI 框架的地方，完美將依賴限制在基礎設施層
	private final ChatMemory chatMemory;

	/**
	 * <b>根據上下文標識 (Context ID) 獲取物理隔離的對話歷史</b>
	 * 
	 * <pre>
	 * 透過複合鍵值從 Redis 或內存中拉取對應的歷史紀錄，並執行 DTO 轉譯。 
	 * 若該上下文尚無任何歷史紀錄，則會安全地回傳不可變的空列表 (Immutable Empty List)，避免 NullPointerException。
	 * </pre>
	 * 
	 * @param contextId 複合式記憶鍵值 (格式建議："conversationId:mode"，以確保不同 AI 人格模式間的記憶絕對隔離)
	 * @return 轉譯後的原始對話訊息視圖列表；若無紀錄則回傳 {@code List.of()}
	 */
	@Override
	public List<RawChatMessageGottenView> getHistory(String contextId) {
		List<Message> messages = chatMemory.get(contextId);

		if (messages == null || messages.isEmpty()) {
			return List.of();
		}

		// 執行防腐層轉換：將 Spring AI 的 Message 隔離，轉為系統內部的 View 送回 Application 層
		return messages.stream().map(msg -> new RawChatMessageGottenView(msg.getMessageType().getValue(),
				msg.getText() != null ? msg.getText() : "")).toList();
	}

	/**
	 * <b>清除特定上下文的對話歷史</b>
	 * 
	 * <pre>
	 * 物理性地刪除該 contextId 底下的所有記憶體暫存。 
	 * 通常觸發於使用者明確發送 /clear 指令、前端重置對話，或連線生命週期結束時的資源回收機制。
	 * </pre>
	 * 
	 * @param contextId 欲清空的複合式記憶鍵值
	 */
	@Override
	public void clear(String contextId) {
		chatMemory.clear(contextId);
	}
}