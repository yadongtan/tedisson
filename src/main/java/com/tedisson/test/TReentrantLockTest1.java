package com.tedisson.test;

import com.tedisson.config.Config;
import com.tedisson.config.Tedisson;
import com.tedisson.config.TedissonClient;
import com.tedisson.lock.TLock;
import com.tedisson.lock.TReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 在 TReentrantLockTest1 和 TReentrantLockTest2 中
 * 模拟分布式环境, 创建了名为lock-test-1的锁, 这两个进程中分别有两个线程, 将去争抢锁
 * 实现同一时刻只有一个线程能获取到锁, 并且当一个jvm进程释放了锁后另一个jvm进程也能及时唤醒线程去抢锁
 */
public class TReentrantLockTest1 {

    private static final Logger log = LoggerFactory.getLogger(TReentrantLockTest1.class);

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
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("线程1释放锁");
            lock.unlock();
        }, "thread-1");

        thread1.start();
        Thread thread2 = new Thread(() -> {
            log.info("线程2等待获取锁");
            lock.lock();
            log.info("线程2获取到锁");
            log.info("线程2释放锁");
            lock.unlock();
        }, "thread-2");

        thread2.start();

        TimeUnit.SECONDS.sleep(100000);
    }

}
