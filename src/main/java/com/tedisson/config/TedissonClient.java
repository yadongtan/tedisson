package com.tedisson.config;

import com.tedisson.lock.HashLockInterface;
import com.tedisson.lock.TLock;
import com.tedisson.lock.TReentrantLock;

/**
 * ά����Redis������, �����𴴽���
 */
public interface TedissonClient {
    TLock getReentrantLock(String lockname);
    TLock getRedLock(String lockname);
}
