package com.tedisson.config;


public class Config {
    public static final String LOCK_PREFIX = "lock:";
    public static final String LOCK_RELEASE_CHANNEL = "tedisson-locks";
    private String host;


    public Config(){
    }

    public String getHost() {
        return host;
    }

    public Config setHost(String host) {
        this.host = host;
        return this;
    }
}
