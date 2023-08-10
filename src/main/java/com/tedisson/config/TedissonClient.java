package com.tedisson.config;

import com.tedisson.lock.TLock;

/**
 * 维护与Redis的连接, 并负责创建锁
 */
public interface TedissonClient {
    TLock getReentrantLock(String lockname);
    TLock getRedLock(String lockname);
}
