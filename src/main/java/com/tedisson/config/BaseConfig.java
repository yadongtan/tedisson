package com.tedisson.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class BaseConfig {
    private static final Logger log = LoggerFactory.getLogger(BaseConfig.class);
    private int idleConnectionTimeout = 10000;
    private int connectTimeout = 10000;
    private int timeout = 3000;
    private int retryAttempts = 3;
    private int retryInterval = 1500;
    private String password;
    private String username;
    private int subscriptionsPerConnection = 5;
    private String clientName;
    private RedisNode channelNodeAddress;


    public int getIdleConnectionTimeout() {
        return idleConnectionTimeout;
    }

    public BaseConfig setIdleConnectionTimeout(int idleConnectionTimeout) {
        this.idleConnectionTimeout = idleConnectionTimeout;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public BaseConfig setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public BaseConfig setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public BaseConfig setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
        return this;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public BaseConfig setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public BaseConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public BaseConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public int getSubscriptionsPerConnection() {
        return subscriptionsPerConnection;
    }

    public BaseConfig setSubscriptionsPerConnection(int subscriptionsPerConnection) {
        this.subscriptionsPerConnection = subscriptionsPerConnection;
        return this;
    }

    public String getClientName() {
        return clientName;
    }

    public BaseConfig setClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public RedisNode getChannelNodeAddress() {
        return channelNodeAddress;
    }

    public void setChannelNodeAddress(RedisNode channelNodeAddress) {
        this.channelNodeAddress = channelNodeAddress;
    }

    public void setChannelNodeAddress(String channelNodeAddress) {
        setChannelNodeAddress(new RedisNode(channelNodeAddress));
    }
}
