package com.tedisson.config;

import com.tedisson.lock.HashLockInterface;
import com.tedisson.lock.TReentrantLock;

/**
 * 维护与Redis的连接, 并负责创建锁
 */
public interface TedissonClient {
    TReentrantLock getLock(String lockname);
}
