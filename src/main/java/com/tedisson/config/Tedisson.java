package com.tedisson.config;

import com.tedisson.lock.HashLockInterface;
import com.tedisson.lock.TReentrantLock;
import com.tedisson.lock.WakeupLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;


public class Tedisson implements TedissonClient {

    private static final Logger logger = LoggerFactory.getLogger(Tedisson.class);
    Config config;
    ConnectionManager connectionManager;
    ConcurrentHashMap<String, WakeupLock> wakeupLockMap;
    private final ConcurrentHashMap<String, Lock> locks;

    public Tedisson(Config config) {
        this.config = config;
        this.connectionManager = new ConnectionManager(config);
        this.locks = new ConcurrentHashMap<>();
        this.wakeupLockMap = new ConcurrentHashMap<>();
        this.connectionManager.subscribeLockChannel(wakeupLockMap, Config.LOCK_RELEASE_CHANNEL);
    }

    public static TedissonClient create(Config config){
        return new Tedisson(config);
    }

    @Override
    public TReentrantLock getLock(String lockname) {
        Lock lock = locks.get(lockname);
        if(lock == null){
            synchronized (locks){
                lock = locks.get(lockname);
                if (lock == null) {
                    HashLockInterface hashLockInterface = new HashLockInterface(this.connectionManager, lockname);
                    TReentrantLock newLock = new TReentrantLock(hashLockInterface);
                    wakeupLockMap.put(hashLockInterface.getLockName(), newLock);
                    locks.put(lockname, newLock);
                    return newLock;
                }
            }
        }
        return (TReentrantLock) lock;
    }
}
