package com.swedapp.bank.service.account;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.swedapp.bank.service.account.errors.LockAcquisitionException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Component
public class AccountLockService {

    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(15);

    // should be replaced with a distributed version in next releases (RDBMS, Redis, Sherlock etc.)
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T withLock(String lockId, Supplier<T> action) {
        var lock = locks.computeIfAbsent(lockId, key -> new ReentrantLock());

        boolean acquired;
        try {
            acquired = lock.tryLock(DEFAULT_LOCK_TIMEOUT.toMillis(), MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("Interrupted while acquiring lock for " + lockId, e);
        }

        if (!acquired) {
            throw new LockAcquisitionException(
                    "Could not acquire lock for " + lockId + " within " + DEFAULT_LOCK_TIMEOUT.toSeconds() + "s");
        }

        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
