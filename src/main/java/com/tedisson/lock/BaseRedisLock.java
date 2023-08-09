package com.tedisson.lock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;

public abstract class BaseRedisLock implements RedisLockInterface{

    protected String lockName;
    protected String hostname = "";
    private static final String DELIMITER = ":";

    public BaseRedisLock(String lockName) {
        this.lockName = lockName;
        InetAddress localHost = null;
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            this.hostname = UUID.randomUUID().toString();
            return;
        }
        this.hostname = localHost.getHostName();
    }

    public String getThreadName(){
        return hostname + DELIMITER + Thread.currentThread();
    }

    // 判断redis上的锁是否被本地线程持有, 因为有可能被其他机器持有
    public boolean isHeldLocally(){
        Map<String, String> map = ConnectionManager.getInstance().getJedisPool().getResource().hgetAll(lockName);
        for (String s : map.keySet()) {
            if(s == null || "".equals(s)){
                continue;
            }
            String[] split = s.split(DELIMITER);
            if (split.length <= 1) {
                continue;
            }else{
                if(hostname.equals(split[0])){
                    return true;
                }
            }
        }
        return false;
    }
}
