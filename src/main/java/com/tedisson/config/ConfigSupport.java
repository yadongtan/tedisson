package com.tedisson.config;


/**
 * ���𴴽�ConnectionManager��һЩ���õĶ���
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

    // У�鵥�������������
    private static void validate(SingleServerConfig config) {
        // У���������޴���
//        if (config.getConnectionPoolSize() < config.getConnectionMinimumIdleSize()) {
//            throw new IllegalArgumentException("connectionPoolSize can't be lower than connectionMinimumIdleSize");
//        }
    }

    // У�鼯Ⱥ�������޴���
    private static void validate(ClusterServersConfig config) {

    }
}
