package com.tedisson.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClusterServersConfig extends BaseConfig{

    private List<RedisNode> nodeAddresses = new ArrayList<>();


    public ClusterServersConfig addNodeAddress(String address, String password){
        this.nodeAddresses.add(new RedisNode(address, password));
        return this;
    }


    public ClusterServersConfig addNodeAddress(String... addresses) {
        for (String address : addresses) {
            this.nodeAddresses.add(new RedisNode(address, null));
        }
        return this;
    }

    public List<RedisNode> getNodeAddresses() {
        return nodeAddresses;
    }

}
