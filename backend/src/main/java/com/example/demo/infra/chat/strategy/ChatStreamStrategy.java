package com.example.demo.infra.chat.strategy;

import reactor.core.publisher.Flux;

/**
 * <b>聊天策略介面標準</b>
 * <p>
 * 定義了 AI 模式的擴充標準。新增任何專家模式 (如 Java 專家、前端專家) 僅需實作此介面並宣告其 Mode 識別碼即可。
 * </p>
 */
public interface ChatStreamStrategy {

	/**
	 * @return 此策略支援的模式字串 (需與前端協定一致)
	 */
	String getSupportedMode();

	/**
	 * 執行串流式 AI 對話
	 * 
	 * @param message 使用者輸入內容
	 * @param chatId  對話上下文 ID (用於 Redis 記憶體檢索)
	 * @return 包含 AI Token 的響應流
	 */
	Flux<String> streamChat(String message, String chatId);
}