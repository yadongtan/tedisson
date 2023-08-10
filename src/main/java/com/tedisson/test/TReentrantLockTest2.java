package com.tedisson.test;


import com.tedisson.config.Config;
import com.tedisson.config.Tedisson;
import com.tedisson.config.TedissonClient;
import com.tedisson.lock.TLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TReentrantLockTest2 {

    private static final Logger log = LoggerFactory.getLogger(TReentrantLockTest1.class);

    public static void main(String[] args) throws InterruptedException {

        // ��������Redis�Ŀ�����ֲ�ʽ��
        Config config = new Config();
        config.useSingleServer().setAddress("120.26.76.100");
        TedissonClient tedissonClient = Tedisson.create(config);
        TLock lock = tedissonClient.getReentrantLock("lock-test-1");

        Thread thread1 = new Thread(() -> {
            System.out.println("�߳�1�ȴ���ȡ��");
            lock.lock();
            System.out.println("�߳�1��ȡ����");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("�߳�1�ͷ���");
            lock.unlock();
        }, "thread-3");

        thread1.start();
        Thread thread2 = new Thread(() -> {
            System.out.println("�߳�2�ȴ���ȡ��");
            lock.lock();
            System.out.println("�߳�2��ȡ����");
            System.out.println("�߳�2�ͷ���");
            lock.unlock();
        }, "thread-4");

        thread2.start();

        TimeUnit.SECONDS.sleep(100000);
    }

}
