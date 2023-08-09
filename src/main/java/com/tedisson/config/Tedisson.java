package com.tedisson.config;

import com.tedisson.lock.HashLockInterface;
import com.tedisson.lock.TReentrantLock;
import com.tedisson.lock.WakeupLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.concurrent.ConcurrentHashMap;


public class Tedisson implements TedissonClient {

    private static final Logger logger = LoggerFactory.getLogger(Tedisson.class);
    Config config;
    ConnectionManager connectionManager;
    ConcurrentHashMap<String, WakeupLock> wakeupLockMap;

    public Tedisson(Config config) {
        this.config = config;
        this.connectionManager = new ConnectionManager(config);
        this.wakeupLockMap = new ConcurrentHashMap<>();
        this.connectionManager.subscribeLockChannel(wakeupLockMap, Config.LOCK_RELEASE_CHANNEL);
    }

    public static TedissonClient create(Config config){
        return new Tedisson(config);
    }

    @Override
    public TReentrantLock getLock(String lockname) {
        return new TReentrantLock(new HashLockInterface(this.connectionManager, lockname));
    }
}
