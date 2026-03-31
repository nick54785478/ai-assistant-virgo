package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.example.demo.iface.handler.ws.ChatWebSocketHandler;

/**
 * <b>WebSocket 通訊配置</b>
 * <p>
 * 啟動 WebSocket 支援，並綁定路由至專屬的處理器 (Handler)，負責提供前端 AI 串流 (Streaming) 輸出的底層通道。
 * </p>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

	private final ChatWebSocketHandler chatWebSocketHandler;

	public WebSocketConfiguration(ChatWebSocketHandler chatWebSocketHandler) {
		this.chatWebSocketHandler = chatWebSocketHandler;
	}

	/**
	 * <b>註冊 WebSocket 端點</b>
	 */
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		// 開放 /chat-ws 端點，並允許跨網域連線 (方便本地測試)
		registry.addHandler(chatWebSocketHandler, "/chat-ws").setAllowedOrigins("*");
	}
}
