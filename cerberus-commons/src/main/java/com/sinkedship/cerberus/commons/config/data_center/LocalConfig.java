package com.sinkedship.cerberus.commons.config.data_center;

import com.sinkedship.cerberus.commons.DataCenter;

/**
 * @author Derrick Guan
 */
public class LocalConfig extends DataCenterConfig {

    private String host = "localhost";
    private int port;

    public LocalConfig() {
        super(DataCenter.LOCAL);
    }

    public LocalConfig setConnectHost(String host) {
        this.host = host;
        return this;
    }

    public String getConnectHost() {
        return host;
    }

    public LocalConfig setConnectPort(int port) {
        this.port = port;
        return this;
    }

    public int getConnectPort() {
        return port;
    }
}
