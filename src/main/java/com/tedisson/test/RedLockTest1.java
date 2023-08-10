package com.tedisson.test;

import com.tedisson.config.Config;
import com.tedisson.config.Tedisson;
import com.tedisson.config.TedissonClient;
import com.tedisson.lock.TLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class RedLockTest1 {
    private static final Logger log = LoggerFactory.getLogger(RedLockTest1.class);
    public static void main(String[] args) throws InterruptedException {
        Config config = new Config();

        config.useClusterServers()
                .addNodeAddress("120.26.76.100")
                .addNodeAddress("redis111.redis.rds.aliyuncs.com", "2209931449Qq")
                .addNodeAddress("r-bp1eexde67z3bsmncrpd.redis.rds.aliyuncs.com", "2209931449Qq")
                .setChannelNodeAddress("120.26.76.100");
        TedissonClient tedissonClient = Tedisson.create(config);


        TLock redLock = tedissonClient.getRedLock("redlock-test");
        new Thread(()->{
            log.info("线程1尝试加锁");
            redLock.lock();
            log.info("线程1加锁成功");
            try {
                TimeUnit.SECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            redLock.unlock();
            log.info("线程1释放锁成功");
        }, "thread-1").start();

        new Thread(()->{
            log.info("线程2尝试加锁");
            redLock.lock();
            log.info("线程2加锁成功");
            try {
                TimeUnit.SECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            redLock.unlock();
            log.info("线程2释放锁成功");
        }, "thread-2").start();

        log.info("连接完成");
    }
}
