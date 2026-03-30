package com.example.demo.application.shared.dto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDataDto {
	
	private String sender; // 對應前端: 'user' | 'bot' | 'system'
	
	private String rawText; // 原始文本
	
	private String thinkingText; // 思考過程 (DeepSeek 專用)
	
	private String outputText; // 最終回答
}