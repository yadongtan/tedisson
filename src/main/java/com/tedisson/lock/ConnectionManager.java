package com.tedisson.lock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ConnectionManager {
    public static final String LOCK_PREFIX = "lock:";
    public static final String LOCK_RELEASE_CHANNEL = "tedisson-locks";
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

                JedisPubSub jedisPubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        WakeupLock wakeupLock = wakeupLockMap.get(message);
                        System.out.println("[CM] " + message + " ���ͷ�");
                        if(wakeupLock != null){
                            wakeupLockMap.remove(message);
                            wakeupLock.wakeup();
                        }
                    }
                };
                jedis.subscribe(jedisPubSub, LOCK_RELEASE_CHANNEL);
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
