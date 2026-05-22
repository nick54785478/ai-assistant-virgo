package com.example.demo.infra.adapter;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.DistributedLockerPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <b>[Infrastructure Adapter] Redis 分散式鎖實作</b>
 * <pre>
 * 本類別為 {@link DistributedLockerPort} 的具體實作，位於六角架構的基礎設施層 (Infrastructure Layer)。
 * 透過 Redisson 提供的 {@link RLock} 機制，確保在叢集環境或高併發場景下（例如：RAG 知識庫文檔解析的雙重檢查鎖），
 * 對共享資源的互斥存取，從而徹底消除競態條件 (Race Condition)。
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class DistributedLockerAdapter implements DistributedLockerPort {

	private final RedissonClient redissonClient;

	/**
	 * <b>在分散式鎖的保護下，執行具有回傳值的業務邏輯</b>
	 * <p>
	 * 採用 {@code tryLock} 機制，若在指定時間內無法獲取鎖，將拋出異常以避免執行緒無限期阻塞。
	 * 執行完畢或發生異常時，會安全地釋放由當前執行緒持有的鎖。
	 * </p>
	 *
	 * @param <T>       任務回傳值的泛型型別
	 * @param key       鎖的唯一鍵值 (資源識別碼，建議加上業務前綴，如 "rag:ingest:docId")
	 * @param waitTime  最大等待獲取鎖的時間 (超過此時間未拿到鎖則放棄並拋出異常)
	 * @param leaseTime 鎖的持有租約時間 (Watchdog 機制，超時自動釋放以防死鎖發生)
	 * @param unit      時間單位 (適用於 waitTime 與 leaseTime)
	 * @param task      受保護的核心業務邏輯 (以 {@link Supplier} 封裝)
	 * @return 任務執行完畢後的結果
	 * @throws RuntimeException 若等待超時無法獲取鎖，或執行緒被中斷時拋出
	 */
	@Override
	public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> task) {
		RLock lock = redissonClient.getLock(key);
		try {
			// 嘗試獲取鎖，若在 waitTime 內拿不到則回傳 false
			if (lock.tryLock(waitTime, leaseTime, unit)) {
				log.debug("成功獲取分散式鎖 [{}]", key);
				return task.get();
			} else {
				log.warn("獲取分散式鎖超時或失敗 [{}]", key);
				throw new RuntimeException("無法獲取分散式鎖: " + key);
			}
		} catch (InterruptedException e) {
			// 恢復中斷狀態，遵循 Java 併發編程的最佳實踐
			Thread.currentThread().interrupt();
			log.error("獲取分散式鎖時執行緒發生中斷 [{}]", key, e);
			throw new RuntimeException("獲取分散式鎖時發生中斷", e);
		} finally {
			// 嚴格檢查：只釋放當前執行緒自己持有的鎖
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
				log.debug("成功釋放分散式鎖 [{}]", key);
			}
		}
	}

	/**
	 * <b>在分散式鎖的保護下，執行無回傳值的業務邏輯</b>
	 * <p>
	 * 此為 {@link #executeWithLock} 的便利方法 (Convenience Method)，適用於不需要回傳結果的 Void 任務。
	 * </p>
	 *
	 * @param key       鎖的唯一鍵值 (資源識別碼)
	 * @param waitTime  最大等待獲取鎖的時間
	 * @param leaseTime 鎖的持有租約時間
	 * @param unit      時間單位
	 * @param task      受保護的核心業務邏輯 (以 {@link Runnable} 封裝)
	 */
	@Override
	public void runWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Runnable task) {
		executeWithLock(key, waitTime, leaseTime, unit, () -> {
			task.run();
			return null; // 配合 Supplier 的語法糖，回傳 null
		});
	}
}