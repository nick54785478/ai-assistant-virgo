package com.example.demo.application.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.port.DistributedLockerPort;
import com.example.demo.infra.idempotence.ProcessedDocument;
import com.example.demo.infra.persistence.ProcessedDocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <b>[Application Service] 知識進食服務</b>
 * <p>
 * 職責：負責將外部文件（如 PDF, TXT）解析、切割並向量化存入 Vector Store。
 * </p>
 * <p>
 * <b>核心特性：</b>
 * </p>
 * <ul>
 * <li><b>分散式冪等性：</b> 透過 SHA-256 檔案 Hash 與 {@link DistributedLockerPort} 實現雙重檢查鎖
 * (Double-Check Locking)，防止多個實例重複處理同一份檔案。</li>
 * <li><b>非同步效能優化：</b> 將耗時的 Tika 解析與 Token 切割置於事務之外，僅在向量庫寫入時開啟事務，最大化 DB
 * 連線效率。</li>
 * <li><b>資源保護：</b> 透過 L1/L2 兩層檢查機制，極大化減少 Embedding API (耗財) 與向量搜尋的重複調用。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionApplicationService {

	private final TransactionTemplate transactionTemplate;
	private final VectorStore vectorStore;
	private final ProcessedDocumentRepository processedDocumentRepository;
	private final DistributedLockerPort lockPort;

	/**
	 * 分散式進食邏輯
	 */
	public void ingest(MultipartFile file) throws IOException {
		// 1. 計算檔案 Hash (外部計算，不佔用鎖資源)
		byte[] fileBytes = file.getBytes();
		String fileHash = DigestUtils.sha256Hex(fileBytes);
		String lockKey = "ingest:lock:" + fileHash;

		// 2. 第一層檢查 (防禦性檢查)
		if (processedDocumentRepository.existsByFileHash(fileHash)) {
			log.info("檔案 [{}] 已存在，跳過。 (L1 Check)", file.getOriginalFilename());
			return;
		}

		// 3. 透過 Port 執行分散式鎖保護的任務
		// 設定等待 10 秒，鎖租約 60 秒 (視 Embedding 速度而定)
		lockPort.runWithLock(lockKey, 10, 60, TimeUnit.SECONDS, () -> {

			// 4. 第二層檢查 (Double-Check)：在鎖內確保此檔案真的沒被處理過
			if (processedDocumentRepository.existsByFileHash(fileHash)) {
				log.info("檔案 [{}] 已由其他實例處理，鎖內跳過。 (L2 Check)", file.getOriginalFilename());
				return;
			}

			// 5. 執行耗時的 RAG 進食邏輯
			this.processAndStore(file, fileBytes, fileHash);
		});
	}

	/**
	 * 真正的業務邏輯處理，確保在事務中完成
	 */
	protected void processAndStore(MultipartFile file, byte[] fileBytes, String fileHash) {
		// 1. 先在交易外做耗時的解析與切割 (不佔用 DB 連線)
		TikaDocumentReader reader = new TikaDocumentReader(new ByteArrayResource(fileBytes));
		List<Document> documents = reader.get();
		TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(800).build();
		List<Document> chunks = splitter.apply(documents);

		// 使用 TransactionTemplate (程式化交易)
		transactionTemplate.executeWithoutResult(status -> {
			log.info("正在為新檔案 [{}] 進行向量化...", file.getOriginalFilename());

			// B. 存入向量庫 (呼叫外部 AI Embedding API)
			vectorStore.accept(chunks);

			// C. 紀錄進食歷史
			processedDocumentRepository.save(ProcessedDocument.builder().fileHash(fileHash)
					.fileName(file.getOriginalFilename()).processedAt(LocalDateTime.now()).build());

			log.info("檔案 [{}] 處理完成並已存檔。", file.getOriginalFilename());
		});
	}
}