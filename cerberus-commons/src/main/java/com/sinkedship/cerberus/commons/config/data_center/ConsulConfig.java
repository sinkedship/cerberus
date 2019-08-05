package com.sinkedship.cerberus.commons.config.data_center;

import com.sinkedship.cerberus.commons.DataCenter;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Derrick Guan
 */
public class ConsulConfig extends DataCenterConfig {

    private static final String DEFAULT_CONSUL_HOST = "localhost";
    private static final int DEFAULT_CONSUL_PORT = 8500;

    private String host = DEFAULT_CONSUL_HOST;
    private int port = DEFAULT_CONSUL_PORT;

    public ConsulConfig() {
        super(DataCenter.CONSUL);
    }

    public ConsulConfig setHost(String host) {
        Preconditions.checkArgument(StringUtils.isNotBlank(host), "Consul host cannot be empty");
        this.host = host;
        return this;
    }

    public ConsulConfig setPort(int port) {
        Preconditions.checkArgument((port >= 0 && port <= 65535),
                "consul connection port should be at range from 0 to 65535");
        this.port = port;
        return this;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

}
