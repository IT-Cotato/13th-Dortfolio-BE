package com.itcotato.dortfolio.domain.record.analysis.service;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RecordAnalysisLockManager {

	private static final int LOCK_STRIPES = 64;

	private final ReentrantLock[] locks = createLocks();

	public void executeWithLock(UUID recordId, Runnable runnable) {
		executeWithLock(recordId, () -> {
			runnable.run();
			return null;
		});
	}

	public <T> T executeWithLock(UUID recordId, Supplier<T> supplier) {
		ReentrantLock lock = lock(recordId);
		lock.lock();
		try {
			return supplier.get();
		} finally {
			lock.unlock();
		}
	}

	public void lockUntilTransactionCompletion(UUID recordId) {
		ReentrantLock lock = lock(recordId);
		lock.lock();
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			lock.unlock();
			throw new IllegalStateException("Transaction synchronization is required for record analysis lock.");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				lock.unlock();
			}
		});
	}

	private static ReentrantLock[] createLocks() {
		ReentrantLock[] createdLocks = new ReentrantLock[LOCK_STRIPES];
		Arrays.setAll(createdLocks, ignored -> new ReentrantLock());
		return createdLocks;
	}

	private ReentrantLock lock(UUID recordId) {
		return locks[Math.floorMod(recordId.hashCode(), locks.length)];
	}
}
