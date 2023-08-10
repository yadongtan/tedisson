package com.tedisson.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TedissonSync extends TedissonAbstractQueueSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(TedissonSync.class);

    private RedisLockInterface lockInterface;

    public TedissonSync(RedisLockInterface lockInterface) {
        this.lockInterface = lockInterface;
    }

    public final void lock() {
        acquire(1);
    }

    public final boolean tryAcquire(int acquires) {
        boolean succ;
        // 会过期的锁
        if (lockInterface instanceof ExpirableLockInterface) {
            ExpirableLockInterface li = (ExpirableLockInterface) lockInterface;
            succ = lockInterface.acquireLock((int) li.getExpirationSeconds());
            if(succ){
                // 判断是否是第一次, 是就添加到看门狗
                if(getOwner() == null || getOwner() != Thread.currentThread()){
                    //添加到看门狗
                    //RenewExpirationManager.EXPIRATION_RENEWAL_MAP.putIfAbsent(li.getCurrentThreadName(), this);
                    RenewExpirationManager.addToWatchDog(Thread.currentThread(), li);
                    log.info("将 " + li.getCurrentThreadName() + " 添加到看门狗");
                }
                setExclusiveOwnerThread(Thread.currentThread());
            }
        } else {
            succ = lockInterface.acquireLock();
            if(succ){
                setExclusiveOwnerThread(Thread.currentThread());
            }
        }
        return succ;
    }

    protected final boolean tryRelease(int releases) {
        int count = lockInterface.releaseLock();
        if(count < 0){
            return true;
        }
        if(count == 0){
//            // 从看门狗中移除
//            if(lockInterface instanceof ExpirableLockInterface){
//                ExpirableLockInterface li = (ExpirableLockInterface) lockInterface;
//                RenewExpirationManager.EXPIRATION_RENEWAL_MAP.remove(li.getCurrentThreadName());
//                log.info("将 " +li.getCurrentThreadName() + " 从看门狗中移除");
//            }
            setExclusiveOwnerThread(null);
            return true;
        }else{
            return false;
        }
    }

    protected final boolean isHeldExclusively() {
        return getExclusiveOwnerThread() == Thread.currentThread();
    }

    final ConditionObject newCondition() {
        return new ConditionObject();
    }

    final Thread getOwner() {
        return getExclusiveOwnerThread();
    }

    final int getHoldCount() {
        return isHeldExclusively() ? getState() : 0;
    }

    final boolean isLocked() {
        return getState() != 0;
    }

    public boolean renewExpiration(){
        if(lockInterface instanceof ExpirableLockInterface){
            ExpirableLockInterface eli = (ExpirableLockInterface) lockInterface;
            String threadName = eli.getThreadName(getOwner());
            return eli.renewExpiration(threadName);
        }else{
            return false;
        }
    }


}
