package com.tedisson.lock;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

public class TReentrantLock implements Lock, WakeupLock{



    private final TReentantLockSync sync;

    public TReentrantLock(String lockname){
        HashLock rLockWithLua = new HashLock(ConnectionManager.getInstance().getJedisPool().getResource(), lockname);
        sync = new TReentantLockSync(this, rLockWithLua);
    }

    private static class TReentantLockSync extends TedissonAbstractQueueSynchronizer {
        TReentrantLock tReentrantLock;
        private HashLock hashLock;

        public TReentantLockSync(TReentrantLock tReentrantLock, HashLock hashLock) {
            this.tReentrantLock = tReentrantLock;
            this.hashLock = hashLock;
            ConnectionManager.getInstance().addWaitingLock(hashLock.lockName, tReentrantLock);
        }

        public final void lock() {
            acquire(1);
        }

        public final boolean tryAcquire(int acquires) {
            boolean succ = hashLock.acquireLock();
            if(succ){
                setExclusiveOwnerThread(Thread.currentThread());
            }

            return succ;
        }

        protected final boolean tryRelease(int releases) {
            int count = hashLock.releaseLock();
            if(count < 0){
                throw new IllegalMonitorStateException();
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

    // 唤醒头节点去抢锁
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


    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                "[Unlocked]" :
                "[Locked by thread " + o.getName() + "]");
    }
}
