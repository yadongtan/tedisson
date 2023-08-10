package com.tedisson.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.concurrent.*;

public class RenewExpirationManager {

    private static final Logger log = LoggerFactory.getLogger(RenewExpirationManager.class);
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);


    public static void addToWatchDog(Thread thread, ExpirableLockInterface ei){
        executor.schedule(()->{
            checkAndRenew(thread, ei);
        }, ei.getExpirationSeconds() >> 1, TimeUnit.SECONDS);
    }


    // �����߼�
    private static void checkAndRenew(Thread thread, ExpirableLockInterface ei){
        log.info(thread.getName() + " ����Ƿ���Ҫ������ع���ʱ��");
        try {
            if(thread.isAlive()){
                boolean b = ei.renewExpiration(ei.getThreadName(thread));
                if (b) {
                    log.info(thread.getName() + ":��");//����������񵽵��ȶ���
                    executor.schedule(()->{
                        checkAndRenew(thread, ei);
                    },  ei.getExpirationSeconds() >> 1, TimeUnit.SECONDS);
                } else {
                    log.info(thread.getName() + ":�����ͷ�, ���ټ��");
                }
            }else{
                log.info(thread.getName() + ":�߳̽���, ���ټ��");
                return;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public RenewExpirationManager() {
        // ÿ��һ��ȥ������������Ƿ�����
//        executor.scheduleWithFixedDelay(() -> {
//            HashMap<String, TedissonSync> removed = new HashMap<>();
//            for (Map.Entry<String, TedissonSync> entry : EXPIRATION_RENEWAL_MAP.entrySet()) {
//                log.info(entry.getKey() + " ����Ƿ���Ҫ������ع���ʱ��");
//                try {
//                    Thread owner = entry.getValue().getOwner();
//                    if(owner != null &&
//                            owner.isAlive()){
//                        boolean b = entry.getValue().renewExpiration();
//                        if (b) {
//                            log.info(entry.getKey() + ":��");
//                        } else {
//                            log.info(entry.getKey() + ":��, ׼���Ƴ�");
//                            removed.putIfAbsent(entry.getKey(), entry.getValue());
//                        }
//                    }else{
//                        removed.putIfAbsent(entry.getKey(), entry.getValue());
//                    }
//                } catch (Throwable throwable) {
//                    throwable.printStackTrace();
//                }
//            }
//            // �Ƴ�
//            for (Map.Entry<String, TedissonSync> entry : removed.entrySet()) {
//                EXPIRATION_RENEWAL_MAP.remove(entry.getKey(), entry.getValue());
//            }
//        }, 1, 1, TimeUnit.SECONDS);

    }

}
