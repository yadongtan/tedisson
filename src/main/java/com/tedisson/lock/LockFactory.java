package com.tedisson.lock;

public class LockFactory {

    // ����hash�Ŀ�������
    public static TReentrantLock getReentrantLock(String lockname){
        return new TReentrantLock(new HashLockInterface(lockname));
    }

    // ���ڴ����������ͽӿڵĿ�������
    public static TReentrantLock getReentrantLock(RedisLockInterface lockInterface){
        return new TReentrantLock(lockInterface);
    }

    // ���Ҫд�����������ݽṹ������ ֻ��Ҫʵ��BaseRedisLockInterface�ķ����� �����뼴��
    // �������String�ṹ��ƣ� ����дһ��StringLockInterface();
    //    public static TReentrantLock getReentrantLock(String lockname){
    //        return new TReentrantLock(new StringLockInterface(lockname));
    //    }
}
