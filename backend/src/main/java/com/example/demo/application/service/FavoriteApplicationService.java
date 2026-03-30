package com.example.demo.application.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.domain.favorite.aggregate.FavoriteMessage;
import com.example.demo.application.shared.dto.view.FavoriteMessageQueriedView;
import com.example.demo.application.shared.page.PagedQueriedView;
import com.example.demo.infra.mapper.FavoriteMessageMapper;
import com.example.demo.infra.persistence.FavoriteMessageRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <b>[Application Service] 收藏紀錄服務</b>
 * <p>
 * 職責：管理使用者收藏的問答對 (QA Pairs)，作為個人對話知識盒的核心。
 * </p>
 * <p>
 * <b>核心特性：</b>
 * </p>
 * <ul>
 * <li><b>領域驅動設計：</b> 透過 {@link FavoriteMessage#create}
 * 工廠方法確保聚合根在初始化時即符合業務規範。</li>
 * <li><b>讀寫分離視圖：</b> 透過 MapStruct 將領域實體轉換為
 * {@link FavoriteMessageQueriedView}，保護內部領域模型不洩漏至前端。</li>
 * <li><b>分頁搜尋：</b> 提供高效的全文檢索與分頁機制。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteApplicationService {

	private final FavoriteMessageMapper mapper;
	private final FavoriteMessageRepository favoriteRepository;

	/**
	 * Use Case: 執行收藏 (QA 對話知識盒)
	 * 
	 * @param question 向 AI 的詢問
	 * @param answer   AI 的答覆
	 * @param chatId   Chat Id
	 * @param tags     給這組對話加上的 tags (可供後續查詢用)
	 * @return 產生的收藏紀錄唯一識別碼
	 */
	@Transactional
	public Long saveToFavorite(String question, String answer, String chatId, List<String> tags) {
		log.info("應用層：正在將問答對存入 PostgreSQL - 來源會話: {}, 標籤數: {}", chatId, (tags != null ? tags.size() : 0));

		// 使用進化後的領域模型工廠方法
		FavoriteMessage favorite = FavoriteMessage.create(question, answer, tags, chatId);

		FavoriteMessage saved = favoriteRepository.save(favorite);

		log.info("問答對已成功收藏，資料庫 ID: {}", saved.getId());
		return saved.getId();
	}

	/**
	 * Use Case: 移除收藏訊息
	 * 
	 * @param id 收藏紀錄 ID
	 * @throws EntityNotFoundException 若 ID 不存在時拋出，符合實體完整性檢查
	 */
	@Transactional
	public void removeFavorite(Long id) {
		log.info("應用層：正在從 PostgreSQL 移除收藏訊息，ID: {}", id);

		// 1. 檢查是否存在 (符合 DDD 嚴謹性)
		if (!favoriteRepository.existsById(id)) {
			throw new EntityNotFoundException("找不到編號為 " + id + " 的收藏紀錄");
		}

		// 2. 執行物理刪除
		favoriteRepository.deleteById(id);
		log.info("收藏紀錄已移除: {}", id);
	}

	/**
	 * Use Case: 獲取分頁後的收藏清單
	 * 
	 * @param keyword 搜尋關鍵字 (支援模糊比對)
	 * @param chatId  會話過濾條件
	 * @param page    頁碼 (從 0 開始)
	 * @param size    每頁筆數
	 */
	@Transactional(readOnly = true)
	public PagedQueriedView<FavoriteMessageQueriedView> getFavorites(String keyword, String chatId, int page,
			int size) {

		// 1. 建立分頁請求 (Spring Data JPA 預設 page 從 0 開始)
		PageRequest pageRequest = PageRequest.of(page, size);

		// 2. 執行底層資料庫查詢
		Page<FavoriteMessage> result = favoriteRepository.searchFavorites(keyword, chatId, pageRequest);

		// 3. 透過 Mapper 批次將 Aggregate 轉換為 Read Model (DTO)
		List<FavoriteMessageQueriedView> favoriteMessageList = mapper.transformAggregate(result.getContent());

		// 轉換為 DTO (Page 介面內建 map 方法，非常優雅)
		// 4. 封裝成自定義的分頁物件
		return PagedQueriedView.<FavoriteMessageQueriedView>builder().content(favoriteMessageList)
				.totalElements(result.getTotalElements()) // 總筆數
				.page(result.getNumber()) // 當前頁碼
				.size(result.getSize()) // 每頁筆數配置
				.build();
	}

	/**
	 * 更新特定收藏的標籤
	 * <p>
	 * 內部調用領域模型 updateTags 方法，確保標籤數量與格式符合業務約束。
	 * </p>
	 * 
	 * @param id   {@link FavoriteMessage} 唯一值
	 * @param tags 要被更新的 Tag 清單(由前端控制要被刪減)
	 */
	@Transactional
	public void updateFavoriteTags(Long id, List<String> tags) {
		favoriteRepository.findById(id).ifPresent(e -> {
			e.updateTags(tags);
			favoriteRepository.save(e);
		});
	}
}
