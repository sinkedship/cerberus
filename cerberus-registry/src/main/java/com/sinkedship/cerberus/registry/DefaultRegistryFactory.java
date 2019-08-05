package com.sinkedship.cerberus.registry;

import com.sinkedship.cerberus.commons.DataCenter;
import com.sinkedship.cerberus.commons.config.data_center.*;
import com.sinkedship.cerberus.commons.exception.CerberusException;
import com.sinkedship.cerberus.core.api.Registry;
import com.sinkedship.cerberus.core.api.RegistryFactory;
import com.sinkedship.cerberus.registry.consul.ConsulRegistry;
import com.sinkedship.cerberus.registry.etcd.EtcdRegistry;
import com.sinkedship.cerberus.registry.local.LocalRegistry;
import com.sinkedship.cerberus.registry.zookeeper.ZookeeperRegistry;

/**
 * Default implementation of {@link RegistryFactory}.
 *
 * @author Derrick Guan
 */
public class DefaultRegistryFactory implements RegistryFactory {

    @Override
    public Registry createRegistry(DataCenterConfig dataCenterConfig) {
        DataCenter dataCenter = dataCenterConfig.getDataCenter();
        switch (dataCenter) {
            case ZOOKEEPER:
                return new ZookeeperRegistry((ZookeeperConfig) dataCenterConfig);
            case CONSUL:
                return new ConsulRegistry((ConsulConfig) dataCenterConfig);
            case ETCD:
                return new EtcdRegistry((EtcdConfig) dataCenterConfig);
            case LOCAL:
                return new LocalRegistry((LocalConfig) dataCenterConfig);
            default:
                throw new CerberusException("Unknown data center:" + dataCenter.name());
        }
    }
}
