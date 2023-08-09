package com.tedisson.test;


import com.tedisson.config.Config;
import com.tedisson.config.Tedisson;
import com.tedisson.config.TedissonClient;
import com.tedisson.lock.TReentrantLock;

import java.util.concurrent.TimeUnit;

public class TReentrantLockTest2 {
    public static void main(String[] args) throws InterruptedException {

        // 创建基于Redis的可重入分布式锁
        Config config = new Config();
        config.setHost("120.26.76.100");
        TedissonClient tedissonClient = Tedisson.create(config);

        TReentrantLock lock = tedissonClient.getLock("lock-test-1");

        Thread thread1 = new Thread(() -> {
            System.out.println("线程1等待获取锁");
            lock.lock();
            System.out.println("线程1获取到锁");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.unlock();
            System.out.println("线程1释放锁");
        }, "thread-3");

        thread1.start();
        Thread thread2 = new Thread(() -> {
            System.out.println("线程2等待获取锁");
            lock.lock();
            System.out.println("线程2获取到锁");
            lock.unlock();
            System.out.println("线程2释放锁");
        }, "thread-4");

        thread2.start();

        TimeUnit.SECONDS.sleep(100000);
    }

}
