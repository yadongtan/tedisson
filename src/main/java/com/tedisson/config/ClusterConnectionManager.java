package com.tedisson.config;

import com.tedisson.lock.WakeupLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.*;
import java.util.concurrent.*;

public class ClusterConnectionManager extends AbstractConnectionManager {


    private static final Logger log = LoggerFactory.getLogger(ClusterConnectionManager.class);
    private ClusterServersConfig clusterServersConfig;
    private Map<String, JedisPool> nodePools;
    private ExecutorService forkExecutor;
    private Integer mMaxTotal; //最大连接数
    private JedisPool channelPool;

    public ClusterConnectionManager(Config config) {
        this.clusterServersConfig = config.getClusterServersConfig();
        nodePools = new HashMap<>();
        mMaxTotal = 10;
        forkExecutor = Executors.newFixedThreadPool(mMaxTotal * clusterServersConfig.getNodeAddresses().size());
        // 创建与channel的连接
        RedisNode channelNodeAddress = config.getClusterServersConfig().getChannelNodeAddress();
        // 配置连接池
        JedisPoolConfig jpc = new JedisPoolConfig();
        jpc.setMaxTotal(10); // 最大连接数
        jpc.setMaxIdle(5);   // 最大空闲连接数;
        // 创建连接池
        channelPool = new JedisPool(jpc, channelNodeAddress.getAddress(), channelNodeAddress.getPort(), 0, channelNodeAddress.getPassword());


        // 创建与每个Redis的连接
        for (RedisNode node : this.clusterServersConfig.getNodeAddresses()) {
            try {
                // 配置连接池
                JedisPoolConfig poolConfig = new JedisPoolConfig();
                poolConfig.setMaxTotal(10); // 最大连接数
                poolConfig.setMaxIdle(5);   // 最大空闲连接数;
                // 创建连接池
                JedisPool jedisPool = new JedisPool(poolConfig, node.getAddress(), node.getPort(), 0, node.getPassword());
                Jedis jedis = jedisPool.getResource();
                jedis.set(node.getAddress(), String.valueOf(node.getPort()));
                log.info("[" + node.toString() + "]连接成功!");
                jedis.close();
                nodePools.put(node.getAddress(), jedisPool);
            }catch (JedisConnectionException e){
                log.info("connected failed, check your node config:" + node);
                throw e;
            }
        }
    }


    @Override
    public JedisPool getChannelJedisPool() {
        return channelPool;
    }


    @Override
    public Object eval(String script, int keyCount, String... params) {
        List<Future<Boolean>> futures = new ArrayList<>();

        for (JedisPool pool : nodePools.values()) {
            Callable<Boolean> task = createTask(pool, script, keyCount, params);
            Future<Boolean> future = forkExecutor.submit(task);
            futures.add(future);
        }

        int successfulCount = 0;
        int requiredSuccess = nodePools.values().size() / 2;  // 需要的成功任务数
        // 统计成功任务数
        for (Future<Boolean> future : futures) {
            try {
                if (future.get()) {
                    successfulCount++;
                }
            } catch (InterruptedException | ExecutionException e) {
                // 处理任务执行异常
                e.printStackTrace();
            }
        }

        // 判断是否任务成功
        if (successfulCount > requiredSuccess) {
            return 1;
        } else {
            return 0;
        }
    }


    private static Callable<Boolean> createTask(JedisPool jedisPool, String script, int keyCount, String... params) {
        return () -> {
            Jedis jedis = null;
            try{
                jedis = jedisPool.getResource();
                Object result = jedis.eval(script, keyCount, params);
                if(result == null){
                    return false;
                }
                return "1".equals(result.toString());
            }finally {
                if(jedis != null){
                    jedis.close();
                }
            }
        };
    }
}
