package com.example.demo.application.port;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <b>[Application Port] 分散式鎖管理接口</b>
 * <p>
 * 定義業務層所需的鎖行為，用於解決分散式環境下的競態條件 (Race Condition)。 常見實作如基於 Redis (Redisson) 或
 * Zookeeper 的分散式鎖。
 * </p>
 */
public interface DistributedLockerPort {

	/**
	 * 在分散式鎖的保護下執行具有回傳值的邏輯
	 * 
	 * @param <T>       任務回傳值的型別
	 * @param key       鎖的唯一鍵值 (資源識別碼)
	 * @param waitTime  最大等待獲取鎖的時間
	 * @param leaseTime 鎖的持有租約時間 (避免死鎖)
	 * @param unit      時間單位 (e.g., TimeUnit.SECONDS)
	 * @param task      受保護的業務邏輯 (Supplier)
	 * @return 任務執行後的結果
	 * @throws RuntimeException 若獲取鎖失敗或執行中斷時拋出
	 */
	<T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> task);

	/**
	 * 在分散式鎖的保護下執行無回傳值的邏輯
	 * 
	 * @param key       鎖的唯一鍵值 (資源識別碼)
	 * @param waitTime  最大等待獲取鎖的時間
	 * @param leaseTime 鎖的持有租約時間
	 * @param unit      時間單位
	 * @param task      受保護的業務邏輯 (Runnable)
	 */
	void runWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Runnable task);
}