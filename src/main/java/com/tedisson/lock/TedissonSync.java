package com.tedisson.lock;

public class TedissonSync extends TedissonAbstractQueueSynchronizer {
    private RedisLockInterface lockInterface;

    public TedissonSync(RedisLockInterface lockInterface) {
        this.lockInterface = lockInterface;
    }

    public final void lock() {
        acquire(1);
    }

    public final boolean tryAcquire(int acquires) {
        boolean succ = lockInterface.acquireLock();
        if(succ){
            setExclusiveOwnerThread(Thread.currentThread());
        }
        return succ;
    }

    protected final boolean tryRelease(int releases) {
        int count = lockInterface.releaseLock();
        if(count < 0){
            return true; //
        }
        if(count == 0){
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
        return getState() == 0 ? null : getExclusiveOwnerThread();
    }

    final int getHoldCount() {
        return isHeldExclusively() ? getState() : 0;
    }

    final boolean isLocked() {
        return getState() != 0;
    }


}
