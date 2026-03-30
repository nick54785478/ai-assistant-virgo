package com.example.demo.application.shared.dto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMetaDto {

	private boolean isThinking; // 歷史紀錄預設為 false

	private Boolean isFavorited; // 這裡先預留，若有跟資料庫 JOIN 可填入

	private Long favoriteId; // 這裡先預留
}
