package com.example.demo.application.shared.command;

import lombok.Data;

@Data
public class SendChatCommand {

	private String conversationId; // 前端產生的持久化對話 ID

	private String mode; // 對應上述的 GENERAL, EXPERT_DDD

	private String text; // 真正的問題內容
}
