package com.tedisson.lock;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public abstract class BaseRedisLockInterface implements RedisLockInterface{

    protected String lockName;
    protected String hostname = "";
    private static final String DELIMITER = ":";

    public BaseRedisLockInterface(String lockName) {
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

    public String getLockName(){
        return lockName;
    }


}
