package com.tedisson.lock;

import com.tedisson.config.ConnectionManager;

public class HashLockInterface extends BaseRedisLockInterface {
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

    public HashLockInterface(ConnectionManager connectionManager, String lockName) {
        super(connectionManager, lockName);
    }


    public boolean acquireLock(int lockExpiry) {
        Object result = connectionManager.eval(LOCK_SCRIPT, 1, lockName, getThreadName(), String.valueOf(lockExpiry));
        return "1".equals(result.toString());
    }

    public boolean acquireLock() {
        return acquireLock(Integer.MAX_VALUE);
    }

    public int releaseLock() {
        Object result = connectionManager.eval(RELEASE_SCRIPT, 1, lockName, getThreadName(), CManager.LOCK_RELEASE_CHANNEL);
        return Integer.parseInt(result.toString());
    }
}
