package com.tedisson.config;

import com.tedisson.lock.WakeupLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    private JedisPool jedisPool;
    private JedisPoolConfig poolConfig;
    private ConcurrentHashMap<String, Jedis> subscribedJedisMap;
    private ConcurrentHashMap<String, JedisPubSub> subscribedJedisPubSubMap;
    private Executor subscribeExecutors;

    public ConnectionManager(Config config){
        subscribeExecutors = Executors.newCachedThreadPool();
        subscribedJedisMap = new ConcurrentHashMap<>();
        subscribedJedisPubSubMap = new ConcurrentHashMap<>();
        // �������ӳ�
        poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10); // ���������
        poolConfig.setMaxIdle(5);   // ������������;
        // �������ӳ�
        jedisPool = new JedisPool(poolConfig, config.getHost());
    }

    // ������
    public void subscribeLockChannel(ConcurrentHashMap<String, WakeupLock> wakeupLockMap, String channel){
        Jedis jedis = jedisPool.getResource();
        JedisPubSub jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                WakeupLock wakeupLock = wakeupLockMap.get(message);
                logger.info("[CM] " + message + " ���ͷ�");
                if (wakeupLock != null) {
                    wakeupLockMap.remove(message);
                    wakeupLock.wakeup();
                }
            }
        };
        subscribedJedisMap.put(channel, jedis);
        subscribedJedisPubSubMap.put(channel, jedisPubSub);
        subscribeExecutors.execute(()->{
            jedis.subscribe(jedisPubSub, Config.LOCK_RELEASE_CHANNEL);
        });
    }

    // ȡ��������
    public void unsubcribeLockChannel( String channel){
        Jedis jedis = subscribedJedisMap.get(channel);
        JedisPubSub jedisPubSub = subscribedJedisPubSubMap.get(channel);
        if(jedisPubSub != null){
            jedisPubSub.unsubscribe();
        }
        if (jedis != null){
            jedis.close();
        }
    }

    public Object eval(String script, int keyCount, String... params){
        Jedis jedis = jedisPool.getResource();
        try{
            Object result = jedis.eval(script, keyCount, params);
            return result;
        }finally {
            jedis.close();
        }

    }
}
