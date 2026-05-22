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
 * <b>[Application Service] RAG 知識庫進食與編排服務</b>
 * <p>
 * 本類別位於六角架構的應用服務層 (Application Layer)，負責編排外部文件（如 PDF, TXT）的解析、 文本切割 (Chunking)
 * 以及向量化存入 Vector Store 的完整工作流 (Workflow)。
 * </p>
 * <b>核心架構亮點與技術難點克服：</b>
 * <ul>
 * <li><b>分散式冪等性 (Idempotency)：</b> 透過檔案 SHA-256 雜湊值與
 * {@link DistributedLockerPort} 實現 雙重檢查鎖 (Double-Check Locking,
 * DCL)，有效防止在叢集部署下，多個實例對同一份大檔案引發的重複解析與重複 Embedding 計費。</li>
 * <li><b>交易邊界極致優化 (Transaction Boundary Optimization)：</b> 捨棄粗粒度的
 * {@code @Transactional}， 改以 {@link TransactionTemplate} 進行程式化控管。將極度耗時的 Tika
 * 文件解析與 Token 切割徹底剝離於 DB 交易之外， 避免長交易 (Long Transaction) 導致資料庫連線池 (Connection
 * Pool) 枯竭。</li>
 * <li><b>防禦性資源保護：</b> 透過 L1 (鎖外) / L2 (鎖內) 兩層緩存穿透檢查，極大化減少昂貴的 AI Embedding API
 * 調用次數。</li>
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
	 * <b>發起分散式進食邏輯 (Ingestion Workflow)</b>
	 * 
	 * <pre>
	 * 接收上傳檔案並執行前置的防禦性檢查。雜湊計算刻意安排在獲取分散式鎖之前執行， 以最小化鎖的佔用時間 (Lock Contention)。
	 * </pre>
	 *
	 * @param file 前端傳入的多媒體檔案
	 * @throws IOException 檔案讀取失敗時拋出
	 */
	public void ingest(MultipartFile file) throws IOException {
		// 1. 計算檔案 Hash：在鎖外執行 CPU 密集型操作，不佔用鎖資源
		byte[] fileBytes = file.getBytes();
		String fileHash = DigestUtils.sha256Hex(fileBytes);
		String lockKey = "ingest:lock:" + fileHash;

		// 2. 第一層檢查 (L1 Check - 快路徑)：無鎖狀態下的初步防禦
		if (processedDocumentRepository.existsByFileHash(fileHash)) {
			log.info("檔案 [{}] 已存在，跳過。 (L1 Check)", file.getOriginalFilename());
			return;
		}

		// 3. 透過 Port 執行分散式鎖保護的任務
		// 設定等待 10 秒，鎖租約 60 秒 (保護機制：避免 Embedding 過程卡死導致死鎖)
		lockPort.runWithLock(lockKey, 10, 60, TimeUnit.SECONDS, () -> {

			// 4. 第二層檢查 (L2 Double-Check)：進入鎖內後的嚴格檢查，確保此檔案未被併發的其他實例處理
			if (processedDocumentRepository.existsByFileHash(fileHash)) {
				log.info("檔案 [{}] 已由其他實例處理，鎖內跳過。 (L2 Check)", file.getOriginalFilename());
				return;
			}

			// 5. 執行核心 RAG 進食邏輯
			this.processAndStore(file, fileBytes, fileHash);
		});
	}

	/**
	 * <b>核心業務處理與程式化交易控管</b>
	 * <p>
	 * 將處理流程嚴格拆分為「無交易狀態的 CPU 密集運算」與「具備交易狀態的 I/O 寫入」， 確保資料庫與向量庫的狀態最終一致性。
	 * </p>
	 *
	 * @param file      原始檔案物件 (用於獲取 metadata)
	 * @param fileBytes 檔案字節陣列
	 * @param fileHash  預先計算好的檔案雜湊值
	 */
	protected void processAndStore(MultipartFile file, byte[] fileBytes, String fileHash) {
		// A. 交易外處理 (無 DB 連線負擔)：執行耗時的文件解析與 Chunking
		log.debug("開始解析檔案並進行 Token 切割 [{}]...", file.getOriginalFilename());
		TikaDocumentReader reader = new TikaDocumentReader(new ByteArrayResource(fileBytes));
		List<Document> documents = reader.get();

		TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(800).build();
		List<Document> chunks = splitter.apply(documents);

		// 使用 TransactionTemplate (程式化交易) 精準框定交易邊界
		transactionTemplate.executeWithoutResult(status -> {
			try {
				log.info("正在為新檔案 [{}] 進行向量化與儲存...", file.getOriginalFilename());

				// B. 存入向量庫：內部封裝了對外部 AI Embedding API 的 HTTP 呼叫與向量寫入
				vectorStore.accept(chunks);

				// C. 紀錄進食歷史：寫入關聯式資料庫 (PostgreSQL)
				processedDocumentRepository.save(ProcessedDocument.builder().fileHash(fileHash)
						.fileName(file.getOriginalFilename()).processedAt(LocalDateTime.now()).build());

				log.info("檔案 [{}] 處理完成並已安全存檔。", file.getOriginalFilename());
			} catch (Exception e) {
				// 若 Embedding 失敗或 DB 寫入失敗，觸發自動回滾 (Rollback)
				status.setRollbackOnly();
				log.error("向量化或存檔過程中發生錯誤，已觸發資料庫回滾 [{}]", file.getOriginalFilename(), e);
				throw e; // 拋出異常交由全域例外處理器接手
			}
		});
	}
}