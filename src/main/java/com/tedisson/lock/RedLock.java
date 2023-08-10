package com.tedisson.lock;

public class RedLock extends TLock{

    public RedLock(RedisLockInterface lockInterface) {
        super(lockInterface);
    }
}
