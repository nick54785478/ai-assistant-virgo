package com.example.demo.infra.chat.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * <b>RAG 知識檢索對話服務</b>
 * 
 * <pre>
 * 負責執行「語義搜尋 + 提示詞增強」的複合邏輯。 
 * 核心流程：使用者提問 -> 向量庫搜尋相似文檔 -> 組裝 System Prompt -> 送入 LLM 串流。
 * </pre>
 */
@Slf4j
@Service
public class ChatRagService {

	private final ChatClient chatClient;
	private final VectorStore vectorStore;
	private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";

	// 注入你在 AiConfiguration 裡定義的 deepseekChatClient Bean
	public ChatRagService(ChatClient deepseekChatClient, VectorStore vectorStore) {
		this.chatClient = deepseekChatClient;
		this.vectorStore = vectorStore;
	}

	/**
	 * 執行 DDD 專家模式的 RAG 對話流
	 * 
	 * @param userId      用戶識別碼 (用於鎖定記憶體)
	 * @param userMessage 使用者提問
	 * @return 附加 Token 統計註腳的內容串流
	 */
	public Flux<String> streamChatWithDddExpert(String userId, String userMessage) {
		log.info("用戶 [{}] 發起專家串流對話: {}", userId, userMessage);

		// 1. RAG 檢索：尋找與使用者問題最相關的 4 筆技術手冊片段
		List<Document> similarDocuments = vectorStore
				.similaritySearch(SearchRequest.builder().query(userMessage).topK(4).build());

		// 2. 拼接上下文內容
		String context = similarDocuments.stream().map(Document::getText).collect(Collectors.joining("\n\n---\n\n"));

		// 3. 呼叫 ChatClient：注入專案規範 context
		return chatClient.prompt().system(s -> s.text("""
				你是一位嚴謹的 DDD 領域專家。請根據下方提供的【專案技術規範】來回答使用者的問題。
				【專案技術規範】：
				{context}
				""").param("context", context)).user(userMessage)
				.advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, userId)).stream().chatResponse()
				.map(this::processResponseWithUsage);
	}

	/**
	 * 內部共用方法：解析 Metadata 中的 Usage 資訊 (Token 統計)
	 */
	private String processResponseWithUsage(ChatResponse response) {
		// 提取 Chunk 文字
		String content = (response.getResult() != null && response.getResult().getOutput().getText() != null)
				? response.getResult().getOutput().getText()
				: "";

		// 當最後一個 Chunk 到達時，metadata 會包含累計的 Token 使用量
		if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
			Usage usage = response.getMetadata().getUsage();
			Integer total = usage.getTotalTokens();
			if (total != null && total > 0) {
				content += String.format("\n\n---\n*📊 專家模式 Token 統計：總消耗 **%d** (輸入: %d / 輸出: %d)*", total,
						usage.getPromptTokens(), usage.getCompletionTokens());
			}
		}
		return content;
	}

}