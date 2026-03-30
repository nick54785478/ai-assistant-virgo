package com.example.demo.infra.persistence;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * <b>[Infrastructure Adapter] Redis 對話記憶儲存庫</b>
 * <p>
 * 實作 Spring AI 的 {@link ChatMemoryRepository} 介面，將對話歷史持久化至 Redis。
 * </p>
 * <p>
 * <b>技術設計亮點：</b>
 * </p>
 * <ul>
 * <li><b>序列化防禦 (Anti-Serialization Issue)：</b> 由於 Spring AI 的 {@code Message}
 * 實作類 (UserMessage, etc.) 缺乏預設建構子且結構複雜，本類透過內建的 {@link MessageDto} 作為中介，確保
 * Jackson 能穩定地進行 JSON 序列化與反序列化。</li>
 * <li><b>自動失效機制 (TTL)：</b> 預設設定為 7 天 (TTL_DAYS)，在節省 Redis
 * 記憶體空間的同時，提供使用者足夠長的歷史回溯期。</li>
 * <li><b>物理隔離：</b> 透過 {@code chat:memory:} 前綴，與 Redis 中的其他業務資料進行物理隔離，避免 Key
 * 衝突。</li>
 * </ul>
 */
@Repository
public class RedisChatMemoryRepository implements ChatMemoryRepository {

	private final RedisTemplate<String, Object> redisTemplate;

	/**
	 * Redis 鍵值前綴
	 */
	private static final String PREFIX = "chat:memory:";
	/**
	 * 紀錄保存期限 (天)
	 */
	private static final long TTL_DAYS = 7;

	public RedisChatMemoryRepository(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * <b>內部防腐層 DTO</b>
	 * <p>
	 * 專門為解決 Jackson 無法直接處理 Spring AI 多型 Message 介面而設計的簡化 POJO。
	 * </p>
	 */
	public static class MessageDto {
		private String role;
		private String content;

		// 關鍵：必須要有預設建構子，Jackson 才能順利反序列化
		public MessageDto() {
		}

		public MessageDto(String role, String content) {
			this.role = role;
			this.content = content;
		}

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}
	}

	/**
	 * 將 Spring AI 訊息物件轉換為可持久化的 DTO
	 */
	private MessageDto toDto(Message message) {
		return new MessageDto(message.getMessageType().getValue(), message.getText());
	}

	/**
	 * <b>角色還原邏輯 (Role Reconstruction)</b>
	 * <p>
	 * 將儲存的字串 Role (USER, ASSISTANT, SYSTEM) 還原為 Spring AI 強型別物件。
	 * </p>
	 */
	private Message toMessage(Object obj) {
		MessageDto dto = (MessageDto) obj;
		// 根據 Role 轉回 Spring AI 對應的物件
		return switch (dto.getRole().toUpperCase()) {
		case "USER" -> new UserMessage(dto.getContent());
		case "ASSISTANT" -> new AssistantMessage(dto.getContent());
		case "SYSTEM" -> new SystemMessage(dto.getContent());
		default -> new UserMessage(dto.getContent());
		};
	}

	/**
	 * 從 Redis 獲取特定會話的所有訊息
	 * 
	 * @return 排序後的訊息列表，若無紀錄則回傳空列表
	 */
	@Override
	public List<String> findConversationIds() {
		Set<String> keys = redisTemplate.keys(PREFIX + "*");
		if (keys == null || keys.isEmpty())
			return List.of();
		return keys.stream().map(key -> key.substring(PREFIX.length())).collect(Collectors.toList());
	}

	/**
	 * 從 Redis 獲取特定會話的所有訊息
	 * 
	 * @return 排序後的訊息列表，若無紀錄則回傳空列表
	 */
	@Override
	public List<Message> findByConversationId(String conversationId) {
		String key = PREFIX + conversationId;
		List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);

		if (rawList == null || rawList.isEmpty()) {
			return List.of();
		}

		// 讀取時：把 DTO 轉換回 Message
		return rawList.stream().map(this::toMessage).collect(Collectors.toList());
	}

	/**
	 * <b>全量儲存對話紀錄</b>
	 * <p>
	 * 每次儲存皆會更新 TTL，實現「滑動視窗」式的保存期限。若 7 天內無互動，該對話將自動移除。
	 * </p>
	 */
	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		String key = PREFIX + conversationId;
		redisTemplate.delete(key);

		if (messages != null && !messages.isEmpty()) {
			// 儲存時：把 Message 轉換為安全的 DTO
			List<MessageDto> dtos = messages.stream().map(this::toDto).collect(Collectors.toList());
			redisTemplate.opsForList().rightPushAll(key, dtos.toArray());
			redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
		}
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		redisTemplate.delete(PREFIX + conversationId);
	}
}