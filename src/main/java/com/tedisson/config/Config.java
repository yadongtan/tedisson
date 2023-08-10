package com.tedisson.config;


public class Config {
    private SingleServerConfig singleServerConfig;
    private ClusterServersConfig clusterServersConfig;
    public static final String LOCK_PREFIX = "lock:";
    public static final String LOCK_RELEASE_CHANNEL = "tedisson-locks";


    public Config(){
    }


    public SingleServerConfig useSingleServer(){
        return this.useSingleServer(new SingleServerConfig());
    }


    SingleServerConfig useSingleServer(SingleServerConfig config) {
        // ºÏ≤È≈‰÷√
        this.checkClusterServersConfig();
        if (this.singleServerConfig == null) {
            this.singleServerConfig = config;
        }
        return this.singleServerConfig;
    }

    public ClusterServersConfig useClusterServers() {
        return this.useClusterServers(new ClusterServersConfig());
    }

    public ClusterServersConfig useClusterServers(ClusterServersConfig config) {
        checkSingleServerConfig();
        if (this.clusterServersConfig == null) {
            this.clusterServersConfig = config;
        }
        return this.clusterServersConfig;
    }

    public void checkSingleServerConfig(){
        if (this.singleServerConfig != null) {
            throw new IllegalStateException("single server config already used!");
        }
    }

    public void checkClusterServersConfig(){
        if (this.clusterServersConfig != null) {
            throw new IllegalStateException("cluster servers config already used!");
        }
    }

    public SingleServerConfig getSingleServerConfig() {
        return singleServerConfig;
    }

    public ClusterServersConfig getClusterServersConfig() {
        return clusterServersConfig;
    }


}
