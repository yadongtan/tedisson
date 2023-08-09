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
            System.out.println("�߳�1�ȴ���ȡ��");
            tReentrantLock.lock();
            System.out.println("�߳�1��ȡ����");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tReentrantLock.unlock();
            System.out.println("�߳�1�ͷ���");
        }, "thread-1");

        thread1.start();
        Thread thread2 = new Thread(() -> {
            System.out.println("�߳�2�ȴ���ȡ��");
            tReentrantLock.lock();
            System.out.println("�߳�2��ȡ����");
            tReentrantLock.unlock();
            System.out.println("�߳�2�ͷ���");
        }, "thread-2");

        thread2.start();

        TimeUnit.SECONDS.sleep(100000);
    }

}
