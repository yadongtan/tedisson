package com.tedisson.test;

import com.tedisson.config.Config;
import com.tedisson.config.Tedisson;
import com.tedisson.config.TedissonClient;
import com.tedisson.lock.TLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedLock {
    private static final Logger log = LoggerFactory.getLogger(RedLock.class);

    public static void main(String[] args) {
        Config config = new Config();

        config.useClusterServers()
                .addNodeAddress("120.26.76.100")
                .addNodeAddress("redis111.redis.rds.aliyuncs.com", "2209931449Qq")
                .addNodeAddress("r-bp1eexde67z3bsmncrpd.redis.rds.aliyuncs.com", "2209931449Qq");
        TedissonClient tedissonClient = Tedisson.create(config);
        TLock redLock = tedissonClient.getRedLock("redlock-test");
        redLock.lock();
        redLock.unlock();
        log.info("连接完成");
    }
}
