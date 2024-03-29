package com.tedisson.lock;

import com.tedisson.config.Config;
import com.tedisson.config.ConnectionManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public abstract class BaseRedisLockInterface implements RedisLockInterface{

    protected String lockName;
    protected String hostname = "";
    private static final String DELIMITER = ":";
    protected ConnectionManager connectionManager;


    public BaseRedisLockInterface(ConnectionManager connectionManager, String lockName) {
        this.connectionManager = connectionManager;
        this.lockName = Config.LOCK_PREFIX + lockName;
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

    public String getCurrentThreadName(){
        return hostname + DELIMITER + Thread.currentThread().getName();
    }

    public String getThreadName(Thread thread){
        return hostname + DELIMITER + thread.getName();
    }

    public String getLockName(){
        return lockName;
    }
}
