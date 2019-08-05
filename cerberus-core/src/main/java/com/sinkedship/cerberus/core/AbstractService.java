package com.sinkedship.cerberus.core;

/**
 * @author Derrick Guan
 */
public abstract class AbstractService extends Service {

    // Version of this service
    protected final int version;

    // Start-up time of this service
    protected final long startUpTs;

    // Is service enable or not
    protected final boolean enable;

    public AbstractService(String identifier, String name, String id, String host, int port,
                    int version, long startUpTs, boolean enable) {
        super(identifier, name, id, host, port);
        this.version = version;
        this.startUpTs = startUpTs;
        this.enable = enable;
    }

    public int getVersion() {
        return version;
    }

    public long getStartUpTs() {
        return startUpTs;
    }

    public boolean isEnable() {
        return enable;
    }
}
