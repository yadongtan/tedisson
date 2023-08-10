package com.tedisson.config;

public class SingleServerConfig extends BaseConfig{

    private String address;

    public String getAddress() {
        return address;
    }

    public SingleServerConfig setAddress(String address) {
        this.address = address;
        return this;
    }
}
