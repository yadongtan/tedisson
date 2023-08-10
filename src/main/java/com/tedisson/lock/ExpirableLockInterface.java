package com.tedisson.lock;

import com.tedisson.config.ConnectionManager;


public abstract class ExpirableLockInterface extends BaseRedisLockInterface{

    private long expiration = 20;   //����ʱ��

    public ExpirableLockInterface(ConnectionManager connectionManager, String lockName) {
        super(connectionManager, lockName);
    }

    public void setExpiration(long expiration){
        this.expiration = expiration;
    }

    public long getExpirationSeconds(){
        return expiration;
    }

    public long getRemainExpirationFromRedis(){
        return connectionManager.keyTTL(getLockName());
    }

    public abstract boolean renewExpiration(String threadName);

}
