package com.example.demo.iface.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.service.FavoriteApplicationService;
import com.example.demo.application.shared.dto.view.FavoriteMessageQueriedView;
import com.example.demo.application.shared.page.PagedQueriedView;
import com.example.demo.iface.dto.req.SaveFavoriteResource;
import com.example.demo.iface.dto.res.FavoriteDeletedResource;
import com.example.demo.iface.dto.res.FavoriteSavedResource;
import com.example.demo.iface.dto.res.PagedFavoriteMessagesQueriedResource;
import com.example.demo.iface.dto.res.TagsUpdatedResource;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/favorite")
public class FavoriteMessageController {

	private FavoriteApplicationService applicationService;

	@PostMapping("")
	public ResponseEntity<FavoriteSavedResource> saveFavorite(@RequestBody SaveFavoriteResource resource) {
		// 安全日誌：摘要問題與回答的前 10 個字，避免日誌過大
		String qSummary = resource.question().substring(0, Math.min(resource.question().length(), 10));
		String aSummary = resource.answer().substring(0, Math.min(resource.answer().length(), 10));

		log.info("⭐ 收到收藏請求 - 問: {}..., 答: {}..., 標籤: {}", qSummary, aSummary, resource.tags());

		// 調用 Service
		Long id = applicationService.saveToFavorite(resource.question(), resource.answer(), resource.chatId(),
				resource.tags());

		return ResponseEntity.ok(new FavoriteSavedResource("200", "Successfully saved QA pair", id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<FavoriteDeletedResource> unfavorite(@PathVariable Long id) {
		log.info("⭐ 收到取消收藏請求 - ID: {}", id);
		applicationService.removeFavorite(id);
		return ResponseEntity.ok(new FavoriteDeletedResource("200", "收藏紀錄已成功移除"));
	}

	/**
	 * 查詢收藏訊息清單 (支援關鍵字搜尋與分頁)
	 * 
	 * @param keyword 關鍵字 (選填，比對問題、回答或標籤)
	 * @param chatId  對話 ID (選填)
	 * @param page    頁碼 (從 0 開始，預設為 0)
	 * @param size    每頁筆數 (預設為 10)
	 * @return 封裝後的 PagedFavoriteMessagesQueriedResource
	 */
	@GetMapping
	public ResponseEntity<PagedFavoriteMessagesQueriedResource> getFavorites(
			@RequestParam(required = false) String keyword, @RequestParam(required = false) String chatId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		PagedQueriedView<FavoriteMessageQueriedView> pagedData = applicationService.getFavorites(keyword, chatId, page,
				size);
		return ResponseEntity.ok(new PagedFavoriteMessagesQueriedResource("200", "查詢成功", pagedData));
	}

	@PatchMapping("/{id}/tags")
	public ResponseEntity<TagsUpdatedResource> updateTags(@PathVariable Long id, @RequestBody List<String> tags) {
		applicationService.updateFavoriteTags(id, tags);
		return ResponseEntity.ok(new TagsUpdatedResource("200", "更新成功"));
	}
}
