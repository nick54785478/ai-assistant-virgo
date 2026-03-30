package com.example.demo.infra.chat.factory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.example.demo.infra.chat.strategy.ChatStreamStrategy;

/**
 * <b>聊天策略工廠</b>
 * <p>
 * 利用 Spring 的依賴注入特性，在啟動時自動蒐集所有 {@link ChatStreamStrategy} 的實作類。 負責根據前端傳入的模式
 * (Mode) 字串，精準分派對應的處理邏輯。
 * </p>
 */
@Component
public class ChatStrategyFactory {

	/**
	 * 執行緒安全的策略快取表
	 */
	private final Map<String, ChatStreamStrategy> strategies = new ConcurrentHashMap<>();

	/**
	 * 自動註冊建構子
	 * 
	 * @param strategyList Spring 自動注入所有 Bean 實作
	 */
	public ChatStrategyFactory(List<ChatStreamStrategy> strategyList) {
		for (ChatStreamStrategy strategy : strategyList) {
			// 將支援的模式轉為大寫作為 Key，確保比對時不因大小寫產生 Bug
			strategies.put(strategy.getSupportedMode().toUpperCase(), strategy);
		}
	}

	/**
	 * 核心路由邏輯
	 * 
	 * @param mode 前端傳入的 mode 字串
	 * @return 符合模式的策略實作；若查無此模式，則強制回傳 "GENERAL" 策略作為防爆機制
	 */
	public ChatStreamStrategy getStrategy(String mode) {
		return strategies.getOrDefault(mode.toUpperCase(), strategies.get("GENERAL"));
	}
}