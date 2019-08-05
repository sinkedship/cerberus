package com.sinkedship.cerberus.commons.config.data_center;

import com.google.common.base.Preconditions;
import com.sinkedship.cerberus.commons.DataCenter;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

/**
 * Zookeeper data center related configuration.
 *
 * @author Derrick Guan
 */
public class ZookeeperConfig extends DataCenterConfig {

    private static final String DEFAULT_ZK_HOST = "localhost";
    private static final int DEFAULT_ZK_PORT = 2181;

    private static final String DEFAULT_CONN_STRING = DEFAULT_ZK_HOST + ":" + DEFAULT_ZK_PORT;
    private static final long DEFAULT_ZK_SESSION_TIMEOUT = 15 * 1000;

    private String connectString;

    // used to connect a single node zookeeper
    private String currentHost = DEFAULT_ZK_HOST;

    // used to connect a single node zookeeper
    private int currentPort = DEFAULT_ZK_PORT;

    private String basePath = "/cerberus";

    // default session time-out
    private long sessionTimeout = DEFAULT_ZK_SESSION_TIMEOUT;

    public ZookeeperConfig() {
        super(DataCenter.ZOOKEEPER);
    }

    /**
     * Set time-out of a zookeeper session
     *
     * @param duration how long to determine a session has been time-out
     * @return this zookeeper config
     */
    public ZookeeperConfig setZkSessionTimeout(Duration duration) {
        Preconditions.checkNotNull(duration, "curator time-out duration cannot be null");
        this.sessionTimeout = duration.toMillis();
        return this;
    }

    /**
     * Return the time-out of a zookeeper session
     *
     * @return the time-out of a zookeeper session
     */
    public long getZkSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Connection string used to connect to zookeeper.
     * <p>
     * Can either connect to a zookeeper cluster or a single zookeeper instance.
     * <p>
     * Takes the form of: $host_1:$port_1,$host_2:$port_2.$host_N:$port_N
     * </p>
     * <p>
     * This method is mutually exclusive with these these methods:
     * {@link ZookeeperConfig#setZkHost(String)} and
     * {@link ZookeeperConfig#setZkPort(int)} and
     * {@link ZookeeperConfig#setZkHostAndPort(String, int)}.
     * <p>
     * If you set connection string with this method at first,
     * then any one of these three methods' invocation will take affects
     * and make the former invocation of this method disable.
     *
     * @param connectString connection string
     * @return this zookeeper config
     */
    public ZookeeperConfig setZkConnectString(String connectString) {
        Preconditions.checkArgument(!StringUtils.isBlank(connectString),
                "zookeeper connection string cannot be empty");
        this.connectString = connectString;
        return this;
    }

    /**
     * Set a single host that used to connect to a single zookeeper instance.
     *
     * @param host zookeeper host
     * @return this zookeeper config
     */
    public ZookeeperConfig setZkHost(String host) {
        return setZkHostAndPort(host, currentPort);
    }

    /**
     * Set a single port that used to connect to a single zookeeper instance.
     *
     * @param port zookeeper port
     * @return this zookeeper config
     */
    public ZookeeperConfig setZkPort(int port) {
        return setZkHostAndPort(currentHost, port);
    }

    /**
     * Set a single host and port that used to connect to a single zookeeper instance.
     *
     * @param host zookeeper host
     * @param port zookeeper port
     * @return this zookeeper configs
     */
    public ZookeeperConfig setZkHostAndPort(String host, int port) {
        Preconditions.checkArgument(!StringUtils.isBlank(host),
                "zookeeper connection host cannot be empty");
        Preconditions.checkArgument((port >= 0 && port <= 65535),
                "zookeeper connection port should be at range from 0 to 65535");
        currentHost = host;
        currentPort = port;
        connectString = currentHost + ":" + currentPort;
        return this;
    }

    /**
     * Current configured connection string used to connect to zookeeper.
     *
     * @return connection string
     */
    public String getConnectString() {
        return connectString == null ? DEFAULT_CONN_STRING : connectString;
    }

    /**
     * All the data will be record under this base path in Zookeeper.
     * <p>
     * It's recommended to set a base path under a exclusive usage namespace.
     *
     * @param basePath default "cerberus"
     * @return this zookeeper config
     */
    public ZookeeperConfig setBasePath(String basePath) {
        Preconditions.checkArgument(!StringUtils.isBlank(basePath),
                "base path cannot be empty");
        this.basePath = basePath;
        return this;
    }

    /**
     * @return base path
     */
    public String getBasePath() {
        return basePath;
    }
}
