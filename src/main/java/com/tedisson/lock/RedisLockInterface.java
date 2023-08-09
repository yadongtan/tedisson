package com.tedisson.lock;

public interface RedisLockInterface {

    boolean acquireLock(int lockExpiry);

    boolean acquireLock();

    int releaseLock();

    String getLockName();
}
