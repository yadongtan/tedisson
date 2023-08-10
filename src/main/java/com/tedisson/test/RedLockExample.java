package com.tedisson.test;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

public class RedLockExample {
    public static void main(String[] args) {
        // 创建 Redisson 客户端实例
        Config config = new Config();
        config.useClusterServers()
                .addNodeAddress("redis://127.0.0.1:7001", "redis://127.0.0.1:7002");
        RedissonClient redisson = Redisson.create(config);

        // 获取 RedLock 锁
        RLock redLock = redisson.getRedLock(redisson.getLock("lock-key"));

        try {
            // 尝试加锁
            if (redLock.tryLock(3, TimeUnit.SECONDS)) {
                // 成功获得锁
                System.out.println("获得锁，执行业务逻辑");
            } else {
                // 无法获得锁
                System.out.println("无法获得锁，可能其他节点持有锁");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 解锁
            redLock.unlock();
        }

        // 关闭 Redisson 客户端
        redisson.shutdown();
    }
}
