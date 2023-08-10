package com.tedisson.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SingleConnectionManager extends AbstractConnectionManager {


    private static final Logger logger = LoggerFactory.getLogger(SingleConnectionManager.class);
    private JedisPool mainJedisPool;
    private ConcurrentHashMap<String, Jedis> subscribedJedisMap;
    private ConcurrentHashMap<String, JedisPubSub> subscribedJedisPubSubMap;
    private Executor subscribeExecutors;

    public SingleConnectionManager(Config config){
        // �������ӳ�
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10); // ���������
        poolConfig.setMaxIdle(5);   // ������������;
        // �������ӳ�
        mainJedisPool = new JedisPool(poolConfig, config.getSingleServerConfig().getAddress());
    }



    public Object eval(String script, int keyCount, String... params){
        Jedis jedis = mainJedisPool.getResource();
        try{
            Object result = jedis.eval(script, keyCount, params);
            return result;
        }finally {
            jedis.close();
        }
    }

    @Override
    public JedisPool getChannelJedisPool() {
        return mainJedisPool;
    }
}
