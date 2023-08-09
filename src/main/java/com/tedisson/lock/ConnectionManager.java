package com.tedisson.lock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionManager {
    public static final String LOCK_RELEASE_QUEUE = "Tedisson-locks";
    private volatile static ConnectionManager instance;
    private final Executor wakeupExecutor = Executors.newSingleThreadExecutor();
    ConcurrentHashMap<String, WakeupLock> wakeupLockMap = new ConcurrentHashMap<>();
    JedisPool jedisPool;
    JedisPoolConfig poolConfig;

    private ConnectionManager() {
        // �������ӳ�
        poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10); // ���������
        poolConfig.setMaxIdle(5);   // ������������;
        // �������ӳ�
        jedisPool = new JedisPool(poolConfig, "120.26.76.100");
        // ����Ŀ�����仯
        wakeupExecutor.execute(()->{
            for(;;){
                Jedis jedis = jedisPool.getResource();
                String lockName = jedis.blpop(0, LOCK_RELEASE_QUEUE).get(1);
                WakeupLock wakeupLock = wakeupLockMap.get(lockName);
                if(wakeupLock != null){
                    wakeupLockMap.remove(lockName);
                    wakeupLock.wakeup();
                }
            }
        });
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public ConnectionManager setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        return this;
    }

    public static ConnectionManager getInstance() {
        if (instance == null) {
            synchronized (ConnectionManager.class) {
                if (instance == null) {
                    instance = new ConnectionManager();
                }
            }
        }
        return instance;
    }


    public void addWaitingLock(String lockName, WakeupLock wakeupLock) {
       this.wakeupLockMap.put(lockName, wakeupLock);

    }
}
