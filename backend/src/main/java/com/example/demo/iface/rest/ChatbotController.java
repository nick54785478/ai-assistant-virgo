package com.example.demo.iface.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.service.ChatApplicationService;
import com.example.demo.application.shared.dto.view.ChatHistoryGottenView;
import com.example.demo.iface.dto.res.ChatClearedResource;
import com.example.demo.iface.dto.res.ChatHistoryGettenResource;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/chat")
public class ChatbotController {

	private ChatApplicationService applicationService;

	/**
	 * 清除記憶接口 (REST) 對應 WebSocket 的 /clear 指令，確保 HTTP 模式也能重置對話
	 */
	@PostMapping("/clear")
	public ResponseEntity<ChatClearedResource> clearChat(HttpSession session,
			@RequestParam(defaultValue = "GENERAL") String mode) {
		// 1. 取得與對話時一致的 Key
		// 若有做模式隔離，這裡要對齊 streamChat 的 ID 生成邏輯
		String chatId = session.getId();
		// 2. 執行物理清理 (這會刪除 Redis 中的 Key)
		applicationService.clearHistory(chatId);
		log.info("使用者 {} 的 {} 模式記憶已重置", chatId, mode);
		// 3. 回傳給前端，讓前端能顯示那一條「系統分隔線」
		return ResponseEntity.ok(new ChatClearedResource("200", "對話記憶已清除，準備開始新的 " + mode + " 對話。"));
	}

	@GetMapping("/history")
	public ResponseEntity<ChatHistoryGettenResource> getHistory(@RequestParam String conversationId,
			@RequestParam String mode) {
		List<ChatHistoryGottenView> historyDtos = applicationService.getChatHistory(conversationId, mode);
		return ResponseEntity.ok(new ChatHistoryGettenResource("200", "獲取歷史紀錄成功", historyDtos));
	}

}