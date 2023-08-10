package com.tedisson.lock;

import com.tedisson.config.ConnectionManager;

public class HashLockInterface extends ExpirableLockInterface {
    private static final String LOCK_SCRIPT =
            "local lockName = KEYS[1]; " +
                    "local threadName = ARGV[1]; " +
                    "local hashKey = lockName; " +
                    "local exists = redis.call('exists', hashKey); " +
                    "if exists == 1 then " +
                    "    if redis.call('hexists', lockName, threadName) == 0 then " +
                    "        return 0; " +
                    "    else" +
                    "        redis.call('hincrby', lockName, threadName, 1); " +
                    "        return 1;" +
                    "    end " +
                    "else " +
                    "    redis.call('hset', lockName, threadName, 1); " +
                    "    redis.call('expire', lockName, tonumber(ARGV[2])); " +
                    "    return 1; " +
                    "end";

    private static final String RELEASE_SCRIPT =
            "if redis.call('hexists', KEYS[1], ARGV[1]) == 1 then " +
                    "    local count = redis.call('hincrby', KEYS[1], ARGV[1], -1); " +
                    "    if count <= 0 then " +
                    "        redis.call('hdel', KEYS[1], ARGV[1]); " +
                    "        redis.call('publish', ARGV[2], KEYS[1]); " +
                    "    end " +
                    "    return count; " +
                    "else " +
                    "    return 0; " +
                    "end";

    // -1代表锁不存在, 0代表不用续锁, 1代表续锁成功
    private static final String RENEW_SCRIPT =
            " local key = KEYS[1]; " +
                    "local field = ARGV[1]; " +
                    "local newTTL = tonumber(ARGV[2]); " +
                    "local exists = redis.call('HEXISTS', key, field); " +
                    "if exists == 1 then " +
                    "   local oldTTL = redis.call('TTL', key); " +
                    "   if oldTTL > 0 and oldTTL < newTTL * 0.5 then " +
                    "       redis.call('EXPIRE', key, newTTL); " +
                    "       return 1; " +
                    "   else " +
                    "       return 0; " +
                    "   end " +
                    "else " +
                    "   return -1; " +
                    "end";


    public HashLockInterface(ConnectionManager connectionManager, String lockName) {
        super(connectionManager, lockName);
    }

    // 刷新锁的时间
    @Override
    public boolean renewExpiration(String threadName) {
        Object res = connectionManager.eval(RENEW_SCRIPT, 1, getLockName(), threadName, String.valueOf(getExpirationSeconds()));
        if(res != null){
            int r = ((Long)res).intValue();
            return r == 1 || r == 0;    //不用续锁或者续锁成功返回true, 返回失败要将锁从看门狗中移除
        }else{
            return false;
        }
    }


    public boolean acquireLock(int lockExpiry) {
        Object result = connectionManager.eval(LOCK_SCRIPT, 1, lockName, getCurrentThreadName(), String.valueOf(lockExpiry));
        return "1".equals(result.toString());
    }

    public boolean acquireLock() {
        return acquireLock((int) getExpirationSeconds());
    }

    public int releaseLock() {
        Object result = connectionManager.eval(RELEASE_SCRIPT, 1, lockName, getCurrentThreadName(), CManager.LOCK_RELEASE_CHANNEL);
        return Integer.parseInt(result.toString());
    }
}
