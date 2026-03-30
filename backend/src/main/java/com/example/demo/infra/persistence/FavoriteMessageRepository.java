package com.example.demo.infra.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.application.domain.favorite.aggregate.FavoriteMessage;

@Repository
public interface FavoriteMessageRepository extends JpaRepository<FavoriteMessage, Long> {

	/**
	 * 根據關鍵字搜尋，比對範圍包含：問題、回答、以及關聯的標籤。
	 * <p>
	 * 使用 LEFT JOIN 確保就算沒有標籤的訊息也能被查出來。
	 * </p>
	 */
	@Query("SELECT DISTINCT f FROM FavoriteMessage f LEFT JOIN f.tags t " + "WHERE (:keyword IS NULL OR :keyword = '' "
			+ "  OR LOWER(f.question) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "  OR LOWER(f.answer) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "  OR LOWER(t) LIKE LOWER(CONCAT('%', :keyword, '%')) " + ") AND (:chatId IS NULL OR f.chatId = :chatId) "
			+ "ORDER BY f.createdAt DESC")
	Page<FavoriteMessage> searchFavorites(@Param("keyword") String keyword, @Param("chatId") String chatId,
			Pageable pageable);
}