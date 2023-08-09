package com.tedisson.lock;

public class LockFactory {

    // 基于hash的可重入锁
    public static TReentrantLock getReentrantLock(String lockname){
        return new TReentrantLock(new HashLockInterface(lockname));
    }

    // 基于传入数据类型接口的可重入锁
    public static TReentrantLock getReentrantLock(RedisLockInterface lockInterface){
        return new TReentrantLock(lockInterface);
    }

    // 如果要写基于其他数据结构的锁， 只需要实现BaseRedisLockInterface的方法， 并传入即可
    // 比如基于String结构设计， 可以写一个StringLockInterface();
    //    public static TReentrantLock getReentrantLock(String lockname){
    //        return new TReentrantLock(new StringLockInterface(lockname));
    //    }
}
