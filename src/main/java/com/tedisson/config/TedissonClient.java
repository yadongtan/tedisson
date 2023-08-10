package com.tedisson.config;

import com.tedisson.lock.TLock;

/**
 * ά����Redis������, �����𴴽���
 */
public interface TedissonClient {
    TLock getReentrantLock(String lockname);
    TLock getRedLock(String lockname);
}
