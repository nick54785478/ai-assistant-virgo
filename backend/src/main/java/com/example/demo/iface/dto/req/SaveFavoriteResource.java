package com.example.demo.iface.dto.req;

import java.util.List;

/**
 * 收藏請求資源
 * 
 * @param question 使用者的提問
 * @param answer   AI 的回答
 * @param tags     使用者自定義標籤 (例如: ["DDD", "Java", "Redis"])
 * @param chatId   來源會話 ID (保留作為追蹤使用)
 */
public record SaveFavoriteResource(String question, String answer, List<String> tags, String chatId) {
}