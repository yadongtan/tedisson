package com.tedisson.lock;

public class TReentrantLock extends BaseLock{

    public TReentrantLock(RedisLockInterface lockInterface) {
        super(lockInterface);
    }
}
