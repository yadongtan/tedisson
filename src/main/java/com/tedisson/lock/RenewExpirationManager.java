package com.tedisson.lock;

import net.bytebuddy.implementation.bytecode.Throw;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class RenewExpirationManager {

    private static final Logger log = LoggerFactory.getLogger(RenewExpirationManager.class);
    // lockName - lock
    protected static final ConcurrentHashMap<String, TedissonSync>
            EXPIRATION_RENEWAL_MAP = new ConcurrentHashMap<>();

    public RenewExpirationManager() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        // ÿ��һ��ȥ������������Ƿ�����
        executor.scheduleWithFixedDelay(() -> {
            HashMap<String, TedissonSync> removed = new HashMap<>();
            for (Map.Entry<String, TedissonSync> entry : EXPIRATION_RENEWAL_MAP.entrySet()) {
                log.info(entry.getKey() + " ����Ƿ���Ҫ������ع���ʱ��");
                try {
                    Thread owner = entry.getValue().getOwner();
                    if(owner != null &&
                            owner.isAlive()){
                        boolean b = entry.getValue().renewExpiration();
                        if (b) {
                            log.info(entry.getKey() + ":��");
                        } else {
                            log.info(entry.getKey() + ":��, ׼���Ƴ�");
                            removed.putIfAbsent(entry.getKey(), entry.getValue());
                        }
                    }else{
                        removed.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
            // �Ƴ�
            for (Map.Entry<String, TedissonSync> entry : removed.entrySet()) {
                EXPIRATION_RENEWAL_MAP.remove(entry.getKey(), entry.getValue());
            }
        }, 1, 1, TimeUnit.SECONDS);

    }

}
