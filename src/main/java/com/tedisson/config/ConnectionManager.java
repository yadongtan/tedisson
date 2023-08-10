package com.tedisson.config;

import com.tedisson.lock.WakeupLock;

import java.util.concurrent.ConcurrentHashMap;

public interface ConnectionManager {

    public void subscribeLockChannel(ConcurrentHashMap<String, WakeupLock> wakeupLockMap, String channel);
    public void unsubcribeLockChannel(String channel);
    public Object eval(String script, int keyCount, String... params);
    public void publishReleasedLock(String message);
    public long keyTTL(String key);
}
