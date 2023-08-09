package com.tedisson.lock;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class TReentrantLockTest {
    public static void main(String[] args) throws InterruptedException {

        TReentrantLock tReentrantLock = new TReentrantLock("lock-test-1");
        Thread thread1 = new Thread(() -> {
            System.out.println("线程1等待获取锁");
            tReentrantLock.lock();
            System.out.println("线程1获取到锁");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tReentrantLock.unlock();
            System.out.println("线程1释放锁");
        }, "thread-1");

        thread1.start();
        Thread thread2 = new Thread(() -> {
            System.out.println("线程2等待获取锁");
            tReentrantLock.lock();
            System.out.println("线程2获取到锁");
            tReentrantLock.unlock();
            System.out.println("线程2释放锁");
        }, "thread-2");

        thread2.start();

        TimeUnit.SECONDS.sleep(100000);
    }

}
