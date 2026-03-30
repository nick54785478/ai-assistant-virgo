package com.example.demo.infra.idempotence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <b>知識文檔處理紀錄</b>
 * <p>
 * 本實體用於追蹤已成功向量化 (Embedded) 並存入 Vector Store 的文件元數據。
 * 主要用途為實現「分散式進食邏輯」的冪等性檢查，避免重複消耗 AI Token 資源。
 * </p>
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "processed_documents")
public class ProcessedDocument {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 檔案指紋 (SHA-256)
	 * <p>
	 * 作為內容唯一性的物理識別碼，用於 Double-Check Locking 機制中的檢索關鍵值。
	 * </p>
	 */
	@Column(unique = true, nullable = false)
	private String fileHash; // 檔案內容的 SHA-256

	/**
	 * 原始上傳檔名 (僅作視覺顯示與維運追蹤用)
	 */
	private String fileName;

	/**
	 * 處理完成的時間戳
	 */
	private LocalDateTime processedAt;
}
