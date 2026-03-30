package com.example.demo.iface.handler.ws;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.demo.application.port.ChatMemoryManagerPort;
import com.example.demo.application.service.ChatApplicationService;
import com.example.demo.application.shared.command.SendChatCommand;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;

/**
 * <b>[展示層 - WebSocket 適配器] 聊天通訊處理器</b>
 * <p>
 * 職責：負責管理 WebSocket 連線生命週期，並將即時通訊協議對接到應用服務層。
 * </p>
 * <p>
 * <b>🚀 維運核心特性：</b>
 * </p>
 * <ul>
 * <li><b>防資源洩漏機制：</b> 當使用者關閉網頁或斷線時，自動銷毀 (dispose) 對應的 Flux 訂閱，立即中止 AI 推理。</li>
 * <li><b>執行緒安全發送：</b> 使用 {@link ConcurrentWebSocketSessionDecorator} 確保多個 Chunk
 * 同時推播時不會發生 Session Crash。</li>
 * <li><b>狀態隔離：</b> 根據前端傳入的 ConversationId 與 Mode 組合複合記憶鍵值。</li>
 * </ul>
 *
 * @author Virgo Project Team
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

	/** 應用層業務服務 */
	private final ChatApplicationService applicationService;
	/** 記憶體管理接口 (Port) */
	private final ChatMemoryManagerPort chatMemory;
	/** JSON 解析器 */
	private final ObjectMapper objectMapper;

	/**
	 * * 活躍訂閱表：用於追蹤每個連線目前正在執行的 Reactive 任務。 Key: Session ID, Value: Disposable
	 * (可用於主動取消任務)
	 */
	private final Map<String, Disposable> activeSubscriptions = new ConcurrentHashMap<>();

	public ChatWebSocketHandler(ChatApplicationService applicationService, ChatMemoryManagerPort chatMemory,
			ObjectMapper objectMapper) {
		this.applicationService = applicationService;
		this.chatMemory = chatMemory;
		this.objectMapper = objectMapper;
	}

	/**
	 * 當連線建立後的初始處理。
	 */
	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		log.info("🟢 WebSocket 連線建立，Session ID: {}", session.getId());
	}

	/**
	 * 核心訊息處理邏輯：
	 * 
	 * <pre>
	 * 1. 封裝安全 Session 
	 * 2. 解析 JSON 指令 
	 * 3. 分配記憶空間 
	 * 4. 觸發 AI 串流。
	 * </pre>
	 */
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		String payload = message.getPayload();
		String sessionId = session.getId();

		// 建立執行緒安全包裝器：設定 10 秒緩衝與 64KB 緩存，避免高併發推播時丟包
		WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(session, 10000, 65536);

		try {
			// 1. 反序列化前端指令
			SendChatCommand requestDto = objectMapper.readValue(payload, SendChatCommand.class);

			// 2. 獲取持久化 ID (若前端未傳送，則退而求其次使用連線 ID)
			String convId = (requestDto.getConversationId() != null && !requestDto.getConversationId().isBlank())
					? requestDto.getConversationId()
					: sessionId;

			// 3. 組合記憶 Key：實現不同 AI 模式間的記憶物理隔離
			String memoryContextId = convId + ":" + requestDto.getMode();

			// 4. 處理特殊清空指令
			if ("/clear".equalsIgnoreCase(requestDto.getText().trim())) {
				chatMemory.clear(memoryContextId);
				sendToClient(safeSession, "[CLEARED]");
				log.info("已清空記憶體區塊: {}", memoryContextId);
				return;
			}

			log.debug("收到 Context {} 的對話請求", memoryContextId);

			// 5. 核心防護：若使用者在 AI 還沒想完前又發了一句，強制中斷舊的 AI 推理以節省 Token
			this.cancelActiveSubscription(sessionId);

			// 6. 開啟 AI 串流流水線
			Disposable subscription = applicationService
					.streamChat(requestDto.getMode(), requestDto.getText(), memoryContextId)
					// 收到 Token 區塊，推回給前端
					.doOnNext(token -> sendToClient(safeSession, token))
					// 異常處理：確保發送結束訊號，避免前端 UI 鎖定
					.doOnError(err -> {
						log.error("串流異常 (Session: {}): {}", sessionId, err.getMessage());
						sendToClient(safeSession, "\n\n🔴 [系統提示] AI 回應意外中斷。");
						sendToClient(safeSession, "[DONE]");
					})
					// 正常完成，發送結束識別符
					.doOnComplete(() -> sendToClient(safeSession, "[DONE]"))
					// 最終清理：從任務表中移除
					.doFinally(signalType -> activeSubscriptions.remove(sessionId)).subscribe();

			// 7. 將訂閱任務納入管理
			activeSubscriptions.put(sessionId, subscription);

		} catch (Exception e) {
			log.error("WebSocket 指令解析失敗 (Session: {}): {}", sessionId, e.getMessage());
			this.handleStreamError(safeSession, e);
		}
	}

	/**
	 * 當連線關閉時觸發（包含主動關閉與異常中斷）。
	 */
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		String sessionId = session.getId();
		log.info("🔴 WebSocket 連線關閉，ID: {}, 原因: {}", sessionId, status.getReason());

		// 極重要：使用者離開了，立刻停掉後台正在算的 AI 任務，釋放記憶體與 Token 成本
		this.cancelActiveSubscription(sessionId);
	}

	/**
	 * 安全地中斷一個正在進行的 Reactive 訂閱任務。
	 */
	private void cancelActiveSubscription(String sessionId) {
		Disposable sub = activeSubscriptions.remove(sessionId);
		if (sub != null && !sub.isDisposed()) {
			sub.dispose();
			log.info("🛑 已回收 Session {} 的 AI 運算資源。", sessionId);
		}
	}

	/**
	 * 底層訊息發送封裝，具備連線狀態檢查。
	 */
	private void sendToClient(WebSocketSession session, String content) {
		try {
			if (session.isOpen()) {
				session.sendMessage(new TextMessage(content));
			}
		} catch (Exception e) {
			log.warn("WS 訊息發送失敗 (Session 已關閉)");
		}
	}

	/**
	 * 系統錯誤的標準化處理。
	 */
	private void handleStreamError(WebSocketSession session, Throwable err) {
		log.error("WebSocket 串流處理發生異常: ", err);
		sendToClient(session, "🔴 [系統錯誤] 目前處理遇到困難，請稍後再試。");
	}
}