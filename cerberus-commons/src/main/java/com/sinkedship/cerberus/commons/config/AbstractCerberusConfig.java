package com.sinkedship.cerberus.commons.config;

import com.sinkedship.cerberus.commons.DataCenter;
import com.sinkedship.cerberus.commons.config.data_center.*;
import com.sinkedship.cerberus.commons.exception.CerberusException;

/**
 * @author Derrick Guan
 */
public abstract class AbstractCerberusConfig {

    private final DataCenterConfig dataCenterConfig;

    public AbstractCerberusConfig(DataCenter dataCenter) {
        switch (dataCenter) {
            case ZOOKEEPER:
                dataCenterConfig = new ZookeeperConfig();
                return;
            case CONSUL:
                dataCenterConfig = new ConsulConfig();
                return;
            case ETCD:
                dataCenterConfig = new EtcdConfig();
                return;
            case LOCAL:
                dataCenterConfig = new LocalConfig();
                return;
            case K8S:
                dataCenterConfig = new K8sConfig();
            default:
                throw new CerberusException("Invalid data center");
        }
    }

    public <C extends DataCenterConfig> C getConcreteDataCenterConfig(Class<C> clz) {
        try {
            return clz.cast(dataCenterConfig);
        } catch (ClassCastException e) {
            throw new CerberusException(e);
        }
    }

    public DataCenterConfig getDataCenterConfig() {
        return dataCenterConfig;
    }
}
