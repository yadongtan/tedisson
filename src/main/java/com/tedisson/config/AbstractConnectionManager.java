package com.tedisson.config;

import com.tedisson.lock.WakeupLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class AbstractConnectionManager implements ConnectionManager {
    private static final Logger log = LoggerFactory.getLogger(AbstractConnectionManager.class);
    protected ConcurrentHashMap<String, Jedis> subscribedJedisMap;
    protected ConcurrentHashMap<String, JedisPubSub> subscribedJedisPubSubMap;
    protected Executor subscribeExecutors;


    public AbstractConnectionManager(){
        subscribeExecutors = Executors.newCachedThreadPool();
        subscribedJedisMap = new ConcurrentHashMap<>();
        subscribedJedisPubSubMap = new ConcurrentHashMap<>();
    }

    public abstract JedisPool getChannelJedisPool();

    // ������
    public void subscribeLockChannel(ConcurrentHashMap<String, WakeupLock> wakeupLockMap, String channel){
        JedisPool channelJedisPool = getChannelJedisPool();
        Jedis jedis = channelJedisPool.getResource();
        JedisPubSub jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                WakeupLock wakeupLock = wakeupLockMap.get(message);
                log.info("[ConnectionManager] " + message + " ���ͷ�");
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

    // ȡ��������
    public void unsubcribeLockChannel(String channel){
        Jedis jedis = subscribedJedisMap.get(channel);
        JedisPubSub jedisPubSub = subscribedJedisPubSubMap.get(channel);
        if(jedisPubSub != null){
            jedisPubSub.unsubscribe();
        }
        if (jedis != null){
            jedis.close();
        }
    }

    public void publishReleasedLock(String message){
        JedisPool jedisPool = getChannelJedisPool();
        if (jedisPool == null) {
            throw new IllegalStateException("Jedis pool is not initialized. ");
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(Config.LOCK_RELEASE_CHANNEL, message);
        }
    }
}
