package com.tedisson.config;

import com.tedisson.lock.HashLockInterface;
import com.tedisson.lock.TReentrantLock;

/**
 * ά����Redis������, �����𴴽���
 */
public interface TedissonClient {
    TReentrantLock getLock(String lockname);
}
