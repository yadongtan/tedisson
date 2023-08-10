package com.tedisson.config;


/**
 * 负责创建ConnectionManager和一些配置的东西
 */
public class ConfigSupport {
    public static ConnectionManager createConnectionManager(Config configCopy){
        if(configCopy.getSingleServerConfig() != null){
            validate(configCopy.getSingleServerConfig());
            return new SingleConnectionManager(configCopy);
        }
        else if(configCopy.getClusterServersConfig() != null){
            validate(configCopy.getClusterServersConfig());
            return new ClusterConnectionManager(configCopy);
        }
        else{
            throw new IllegalArgumentException("invalid config for connection");
        }
    }

    // 校验单机配置有误错误
    private static void validate(SingleServerConfig config) {
        // 校验配置有无错误
//        if (config.getConnectionPoolSize() < config.getConnectionMinimumIdleSize()) {
//            throw new IllegalArgumentException("connectionPoolSize can't be lower than connectionMinimumIdleSize");
//        }
    }

    // 校验集群配置有无错误
    private static void validate(ClusterServersConfig config) {

    }
}
