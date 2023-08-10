package com.tedisson.lock;

import com.tedisson.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



// 不可重入的红锁接口实现
public class StringRedLockInterface extends BaseRedisLockInterface {


    private static final Logger log = LoggerFactory.getLogger(StringRedLockInterface.class);

    // 加锁成功返回1, 失败返回0
    private static final String LOCK_SCRIPT =
            "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then "+
            "redis.call('expire', KEYS[1], ARGV[2]) return 1 else return 0 end";

    // 释放锁成功返回1, 失败返回0
    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
            + "return redis.call('del', KEYS[1]) else return 0 end";

    public StringRedLockInterface(ConnectionManager connectionManager, String lockName) {
        super(connectionManager, lockName);
    }

    @Override
    public boolean acquireLock(int lockExpiry) {
        Object result = connectionManager.eval(LOCK_SCRIPT, 1, getLockName(), getThreadName(), String.valueOf(lockExpiry));
        if(result != null){
            if((int)result == 1){
                return true;
            }else{
                releaseLock();  //释放自己占有的资源
                return false;
            }
        }
        releaseLock();  //释放自己占有的资源
        return false; //获取锁失败, 说明本来就没拿到锁
    }

    @Override
    public boolean acquireLock() {
        return acquireLock(Integer.MAX_VALUE);
    }

    // 始终返回0, 意思是释放锁成功, 以保证能把唤醒节点去抢锁
    @Override
    public int releaseLock() {
        Object result = connectionManager.eval(RELEASE_SCRIPT, 1, getLockName(), getThreadName());
        if(result != null && (int)result == 1){
            connectionManager.publishReleasedLock(getLockName());
        }
        return 0;
    }
}
