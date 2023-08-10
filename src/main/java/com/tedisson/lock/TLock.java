package com.tedisson.lock;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

public class TLock implements Lock, WakeupLock{

    private final TedissonSync sync;

    public TLock(RedisLockInterface lockInterface) {
        // ����ͬ������
        sync = new TedissonSync(lockInterface);
    }


    public void lock() {
        sync.lock();
    }


    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }


    public boolean tryLock() {
        return sync.tryAcquire(1);
    }


    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }



    public void unlock() {
        sync.release(1);
    }

    // ����ͷ�ڵ�ȥ����
    public void wakeup(){
        sync.unparkSuccessor();
    }

    public Condition newCondition() {
        return sync.newCondition();
    }


    public int getHoldCount() {
        return sync.getHoldCount();
    }


    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }


    public boolean isLocked() {
        return sync.isLocked();
    }


    protected Thread getOwner() {
        return sync.getOwner();
    }

    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }


    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    public final int getQueueLength() {
        return sync.getQueueLength();
    }


    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }


    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof TedissonAbstractQueueSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((TedissonAbstractQueueSynchronizer.ConditionObject)condition);
    }


    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof TedissonAbstractQueueSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((TedissonAbstractQueueSynchronizer.ConditionObject)condition);
    }


    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof TedissonAbstractQueueSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((TedissonAbstractQueueSynchronizer.ConditionObject)condition);
    }

    public Thread getOwnerThread(){
        return sync.getOwner();
    }

    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                "[Unlocked]" :
                "[Locked by thread " + o.getName() + "]");
    }
}
