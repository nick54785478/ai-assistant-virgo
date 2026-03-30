package com.example.demo.application.port;

import java.util.List;

import com.example.demo.application.shared.dto.view.RawChatMessageGottenView;

/**
 * <b>[Application Port] 對話記憶管理接口</b>
 * <p>
 * 定義了對話紀錄持久化與檢索的標準規範。 透過此接口，應用層無需知曉底層是使用 Redis、DB 或是 Spring AI 的記憶體實作。
 * </p>
 */
public interface ChatMemoryManagerPort {

	/**
	 * 根據上下文標識獲取對話歷史 * @param contextId 複合式記憶鍵值 (例如: "conversationId:mode")
	 * 
	 * @return 原始對話訊息列表，若無紀錄則回傳空列表
	 */
	List<RawChatMessageGottenView> getHistory(String contextId);

	/**
	 * 清除特定上下文的對話歷史 通常用於使用者發送 /clear 指令或重置對話時 * @param contextId 欲清空的複合式記憶鍵值
	 */
	void clear(String contextId);
}