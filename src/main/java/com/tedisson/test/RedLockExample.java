package com.tedisson.test;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

public class RedLockExample {
    public static void main(String[] args) {
        // ���� Redisson �ͻ���ʵ��
        Config config = new Config();
        config.useClusterServers()
                .addNodeAddress("redis://127.0.0.1:7001", "redis://127.0.0.1:7002");
        RedissonClient redisson = Redisson.create(config);

        // ��ȡ RedLock ��
        RLock redLock = redisson.getRedLock(redisson.getLock("lock-key"));

        try {
            // ���Լ���
            if (redLock.tryLock(3, TimeUnit.SECONDS)) {
                // �ɹ������
                System.out.println("�������ִ��ҵ���߼�");
            } else {
                // �޷������
                System.out.println("�޷�����������������ڵ������");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // ����
            redLock.unlock();
        }

        // �ر� Redisson �ͻ���
        redisson.shutdown();
    }
}
