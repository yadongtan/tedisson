package com.tedisson.test;


import com.tedisson.config.Config;
import com.tedisson.config.Tedisson;
import com.tedisson.config.TedissonClient;
import com.tedisson.lock.TLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TReentrantLockTest2 {

    private static final Logger log = LoggerFactory.getLogger(TReentrantLockTest2.class);

    public static void main(String[] args) throws InterruptedException {

        // 创建基于Redis的可重入分布式锁
        Config config = new Config();
        config.useSingleServer().setAddress("120.26.76.100");
        TedissonClient tedissonClient = Tedisson.create(config);
        TLock lock = tedissonClient.getReentrantLock("lock-test-1");

        Thread thread1 = new Thread(() -> {
            log.info("线程1等待获取锁");
            lock.lock();
            log.info("线程1获取到锁");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("线程1释放锁");
            lock.unlock();
        }, "thread-3");

        thread1.start();
        Thread thread2 = new Thread(() -> {
            log.info("线程2等待获取锁");
            lock.lock();
            log.info("线程2获取到锁");
            log.info("线程2释放锁");
            lock.unlock();
        }, "thread-4");

        thread2.start();

        TimeUnit.SECONDS.sleep(100000);
    }

}
