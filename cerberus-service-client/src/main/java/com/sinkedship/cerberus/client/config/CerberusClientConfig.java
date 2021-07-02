package com.sinkedship.cerberus.client.config;

import io.airlift.drift.transport.netty.client.DriftNettyClientConfig;
import com.sinkedship.cerberus.commons.config.AbstractCerberusConfig;
import com.sinkedship.cerberus.commons.DataCenter;
import com.sinkedship.cerberus.commons.config.data_center.DataCenterConfig;
import io.airlift.drift.transport.netty.client.DriftNettyConnectionFactoryConfig;

/**
 * Client configuration used to discover dedicated Thrift services.
 * <p>
 * This class contains three independent configuration:
 * <p>
 * a concrete implementation of {@link DataCenterConfig} and
 * a drift netty client config of {@link io.airlift.drift.transport.netty.client.DriftNettyClientConfig}
 * a drift netty connection factory config of {@link io.airlift.drift.transport.netty.client.DriftNettyConnectionFactoryConfig}
 *
 * @author Derrick Guan
 */
public final class CerberusClientConfig extends AbstractCerberusConfig {

    private final DriftNettyClientConfig driftNettyClientConfig;

    private final DriftNettyConnectionFactoryConfig connectionFactoryConfig;

    public CerberusClientConfig(DataCenter dataCenter) {
        super(dataCenter);
        driftNettyClientConfig = new DriftNettyClientConfig();
        connectionFactoryConfig = new DriftNettyConnectionFactoryConfig();
    }

    public CerberusClientConfig(DataCenter dataCenter,
                                DriftNettyClientConfig driftNettyClientConfig,
                                DriftNettyConnectionFactoryConfig connectionFactoryConfig
                                ) {
        super(dataCenter);
        this.driftNettyClientConfig = driftNettyClientConfig;
        this.connectionFactoryConfig = connectionFactoryConfig;
    }

    public DriftNettyClientConfig getDriftNettyClientConfig() {
        return driftNettyClientConfig;
    }

    public DriftNettyConnectionFactoryConfig getDriftNettyConnFactoryConfig() {
        return connectionFactoryConfig;
    }
}
