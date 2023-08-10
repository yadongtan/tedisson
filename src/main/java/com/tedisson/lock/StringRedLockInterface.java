package com.tedisson.lock;

import com.tedisson.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



// ��������ĺ����ӿ�ʵ��
public class StringRedLockInterface extends BaseRedisLockInterface {


    private static final Logger log = LoggerFactory.getLogger(StringRedLockInterface.class);

    // �����ɹ�����1, ʧ�ܷ���0
    private static final String LOCK_SCRIPT =
            "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then "+
            "redis.call('expire', KEYS[1], ARGV[2]) return 1 else return 0 end";

    // �ͷ����ɹ�����1, ʧ�ܷ���0
    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
            + "return redis.call('del', KEYS[1]) else return 0 end";

    public StringRedLockInterface(ConnectionManager connectionManager, String lockName) {
        super(connectionManager, lockName);
    }

    @Override
    public boolean acquireLock(int lockExpiry) {
        Object result = connectionManager.eval(LOCK_SCRIPT, 1, getLockName(), getCurrentThreadName(), String.valueOf(lockExpiry));
        if(result != null){
            if((int)result == 1){
                return true;
            }else{
                releaseLock();  //�ͷ��Լ�ռ�е���Դ
                return false;
            }
        }
        releaseLock();  //�ͷ��Լ�ռ�е���Դ
        return false; //��ȡ��ʧ��, ˵��������û�õ���
    }

    @Override
    public boolean acquireLock() {
        return acquireLock(Integer.MAX_VALUE);
    }

    // ʼ�շ���0, ��˼���ͷ����ɹ�, �Ա�֤�ܰѻ��ѽڵ�ȥ����
    @Override
    public int releaseLock() {
        Object result = connectionManager.eval(RELEASE_SCRIPT, 1, getLockName(), getCurrentThreadName());
        if(result != null && (int)result == 1){
            connectionManager.publishReleasedLock(getLockName());
        }
        return 0;
    }

}
