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


    // 续锁逻辑
    private static void checkAndRenew(Thread thread, ExpirableLockInterface ei){
        log.info(thread.getName() + " 检测是否需要继续监控过期时间");
        try {
            if(thread.isAlive()){
                boolean b = ei.renewExpiration(ei.getThreadName(thread));
                if (b) {
                    log.info(thread.getName() + ":是");//重新添加任务到调度队列
                    executor.schedule(()->{
                        checkAndRenew(thread, ei);
                    },  ei.getExpirationSeconds() >> 1, TimeUnit.SECONDS);
                } else {
                    log.info(thread.getName() + ":锁已释放, 不再监控");
                }
            }else{
                log.info(thread.getName() + ":线程结束, 不再监控");
                return;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public RenewExpirationManager() {
        // 每隔一秒去检测锁持有者是否正常
//        executor.scheduleWithFixedDelay(() -> {
//            HashMap<String, TedissonSync> removed = new HashMap<>();
//            for (Map.Entry<String, TedissonSync> entry : EXPIRATION_RENEWAL_MAP.entrySet()) {
//                log.info(entry.getKey() + " 检测是否需要继续监控过期时间");
//                try {
//                    Thread owner = entry.getValue().getOwner();
//                    if(owner != null &&
//                            owner.isAlive()){
//                        boolean b = entry.getValue().renewExpiration();
//                        if (b) {
//                            log.info(entry.getKey() + ":是");
//                        } else {
//                            log.info(entry.getKey() + ":否, 准备移除");
//                            removed.putIfAbsent(entry.getKey(), entry.getValue());
//                        }
//                    }else{
//                        removed.putIfAbsent(entry.getKey(), entry.getValue());
//                    }
//                } catch (Throwable throwable) {
//                    throwable.printStackTrace();
//                }
//            }
//            // 移除
//            for (Map.Entry<String, TedissonSync> entry : removed.entrySet()) {
//                EXPIRATION_RENEWAL_MAP.remove(entry.getKey(), entry.getValue());
//            }
//        }, 1, 1, TimeUnit.SECONDS);

    }

}
