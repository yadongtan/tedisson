package com.tedisson.test;


import com.tedisson.lock.LockFactory;
import com.tedisson.lock.TReentrantLock;

import java.util.concurrent.TimeUnit;

public class TReentrantLockTest2 {
    public static void main(String[] args) throws InterruptedException {

        // ��������Redis�Ŀ�����ֲ�ʽ��
        TReentrantLock lock = LockFactory.getReentrantLock("lock-test-1");

        Thread thread1 = new Thread(() -> {
            System.out.println("�߳�1�ȴ���ȡ��");
            lock.lock();
            System.out.println("�߳�1��ȡ����");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.unlock();
            System.out.println("�߳�1�ͷ���");
        }, "thread-3");

        thread1.start();
        Thread thread2 = new Thread(() -> {
            System.out.println("�߳�2�ȴ���ȡ��");
            lock.lock();
            System.out.println("�߳�2��ȡ����");
            lock.unlock();
            System.out.println("�߳�2�ͷ���");
        }, "thread-4");

        thread2.start();

        TimeUnit.SECONDS.sleep(100000);
    }

}
