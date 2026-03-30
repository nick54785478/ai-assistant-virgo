package com.example.demo.application.domain.favorite.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <b>[Domain Aggregate Root] 收藏對話訊息</b>
 * <p>
 * 代表使用者主動標記並儲存的「問答對 (QA Pair)」，本類別採用「嚴格封裝」策略，杜絕外部透過 {@code new} 或
 * {@code Builder} 直接建立不完整的實體。
 * </p>
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "favorite_messages")
public class FavoriteMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 使用者的提問內容
	 */
	@Column(columnDefinition = "TEXT")
	private String question; // 使用者問了什麼

	/**
	 * AI 產出的原始回答 (包含 Markdown 標籤)
	 */
	@Column(columnDefinition = "TEXT")
	private String answer; // AI 答了什麼

	/**
	 * 自定義標籤清單
	 * <p>
	 * 使用 ElementCollection 儲存。注意：更新時需透過 {@link #updateTags} 確保集合的可變性。
	 * </p>
	 */
	@ElementCollection
	@CollectionTable(name = "favorite_tags", joinColumns = @JoinColumn(name = "favorite_id"))
	@Column(name = "tag")
	private List<String> tags = new ArrayList<>(); // 標籤存儲

	/**
	 * 關聯的原始會話 ID (UUID)
	 */
	private String chatId;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	/**
	 * <b>靜態工廠方法 (Static Factory Method)</b>
	 * <p>
	 * 優點：命名具備業務語意，並在進入建構子前執行初步的「不變量 (Invariant)」檢查。
	 * </p>
	 *
	 * @param question 提問內容 (不可為空)
	 * @param answer   回答內容 (不可為空)
	 * @param tags     初始標籤
	 * @param chatId   來源會話識別碼
	 * @return 符合領域規則的 FavoriteMessage 實例
	 */
	public static FavoriteMessage create(String question, String answer, List<String> tags, String chatId) {
		if (question == null || question.isBlank()) {
			throw new IllegalArgumentException("問題不能為空");
		}
		if (answer == null || answer.isBlank()) {
			throw new IllegalArgumentException("回答不能為空");
		}

		return new FavoriteMessage(question, answer, tags, chatId);
	}

	/**
	 * <b>私有建構子</b>
	 * <p>
	 * 強制所有實體建立都必須經過 {@link #create} 工廠方法，確保領域規則不被繞過。
	 * </p>
	 */
	private FavoriteMessage(String question, String answer, List<String> tags, String chatId) {
		this.question = question;
		this.answer = answer;
		this.tags = tags != null ? tags : new ArrayList<>();
		this.chatId = chatId;
		this.createdAt = LocalDateTime.now();
	}

	/**
	 * <b>業務邏輯：更新標籤清單</b>
	 * 
	 * <pre>
	 * 包含以下約束：
	 * 1. 標籤數量上限為 10 個。
	 * 2. 自動過濾空白字元與重複項。
	 * 註：顯式建立新的 ArrayList 以避免 Hibernate 拋出 UnsupportedOperationException。
	 * </pre>
	 *
	 * @param newTags 使用者輸入的新標籤清單
	 */
	public void updateTags(List<String> newTags) {
		if (newTags != null && newTags.size() > 10) {
			throw new IllegalArgumentException("標籤數量不能超過 10 個");
		}

		// 使用 new ArrayList<>() 包起來，確保它是可變的
		this.tags = new ArrayList<>(newTags.stream().filter(t -> t != null && !t.isBlank()).distinct().toList());
	}
}