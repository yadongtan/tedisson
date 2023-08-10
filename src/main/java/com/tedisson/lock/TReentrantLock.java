package com.tedisson.lock;

public class TReentrantLock extends TLock {

    public TReentrantLock(RedisLockInterface lockInterface) {
        super(lockInterface);
    }
}
