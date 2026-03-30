package com.example.demo.infra.adapter;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.DistributedLockerPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class DistributedLockerAdapter implements DistributedLockerPort {

	private final RedissonClient redissonClient;

	@Override
	public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> task) {
		RLock lock = redissonClient.getLock(key);
		try {
			if (lock.tryLock(waitTime, leaseTime, unit)) {
				return task.get();
			} else {
				throw new RuntimeException("無法獲取分散式鎖: " + key);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("獲取鎖時發生中斷", e);
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	@Override
	public void runWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Runnable task) {
		executeWithLock(key, waitTime, leaseTime, unit, () -> {
			task.run();
			return null;
		});
	}
}