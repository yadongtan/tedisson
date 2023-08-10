package com.tedisson.config;

public class RedisNode {
    private String address;
    private String password;
    private int port = 6379;

    public RedisNode(String address, String password, int port) {
        this.address = address;
        this.password = password;
        this.port = port;
    }

    public RedisNode(String address, String password) {
        this.address = address;
        this.password = password;
    }

    public RedisNode(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "RedisNode{" +
                "address='" + address + '\'' +
                ", password='" + password + '\'' +
                ", port=" + port +
                '}';
    }
}
