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
        // 配置连接池
        poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10); // 最大连接数
        poolConfig.setMaxIdle(5);   // 最大空闲连接数;
        // 创建连接池
        jedisPool = new JedisPool(poolConfig, config.getHost());
    }

    // 监视锁
    public void subscribeLockChannel(ConcurrentHashMap<String, WakeupLock> wakeupLockMap, String channel){
        Jedis jedis = jedisPool.getResource();
        JedisPubSub jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                WakeupLock wakeupLock = wakeupLockMap.get(message);
                logger.info("[ConnectionManager] " + message + " 已释放");
                if (wakeupLock != null) {
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

    // 取消监视锁
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
