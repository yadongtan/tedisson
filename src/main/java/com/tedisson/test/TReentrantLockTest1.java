package com.tedisson.test;

import com.tedisson.lock.LockFactory;
import com.tedisson.lock.TReentrantLock;

import java.util.concurrent.TimeUnit;

public class TReentrantLockTest1 {
    public static void main(String[] args) throws InterruptedException {

        // 创建基于Redis的可重入分布式锁
        TReentrantLock lock = LockFactory.getReentrantLock("lock-test-1");

        Thread thread1 = new Thread(() -> {
            System.out.println("线程1等待获取锁");
            lock.lock();
            System.out.println("线程1获取到锁");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.unlock();
            System.out.println("线程1释放锁");
        }, "thread-1");

        thread1.start();
        Thread thread2 = new Thread(() -> {
            System.out.println("线程2等待获取锁");
            lock.lock();
            System.out.println("线程2获取到锁");
            lock.unlock();
            System.out.println("线程2释放锁");
        }, "thread-2");

        thread2.start();

        TimeUnit.SECONDS.sleep(100000);
    }

}
