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
 * �� TReentrantLockTest1 �� TReentrantLockTest2 ��
 * ģ��ֲ�ʽ����, ��������Ϊlock-test-1����, �����������зֱ��������߳�, ��ȥ������
 * ʵ��ͬһʱ��ֻ��һ���߳��ܻ�ȡ����, ���ҵ�һ��jvm�����ͷ���������һ��jvm����Ҳ�ܼ�ʱ�����߳�ȥ����
 */
public class TReentrantLockTest1 {

    private static final Logger log = LoggerFactory.getLogger(TReentrantLockTest1.class);

    public static void main(String[] args) throws InterruptedException {

        // ��������Redis�Ŀ�����ֲ�ʽ��
        Config config = new Config();
        config.useSingleServer().setAddress("120.26.76.100");
        TedissonClient tedissonClient = Tedisson.create(config);

        TLock lock = tedissonClient.getReentrantLock("lock-test-1");


        Thread thread1 = new Thread(() -> {
            log.info("�߳�1�ȴ���ȡ��");
            lock.lock();
            log.info("�߳�1��ȡ����");
            try {
                TimeUnit.SECONDS.sleep(60);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("�߳�1�ͷ���");
            lock.unlock();
        }, "thread-1");

        thread1.start();
//        Thread thread2 = new Thread(() -> {
//            log.info("�߳�2�ȴ���ȡ��");
//            lock.lock();
//            log.info("�߳�2��ȡ����");
//            log.info("�߳�2�ͷ���");
//            lock.unlock();
//        }, "thread-2");
//
//        thread2.start();

        TimeUnit.SECONDS.sleep(100000);
    }

}
