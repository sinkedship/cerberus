package com.sinkedship.cerberus.client.config;

import io.airlift.drift.transport.netty.client.DriftNettyClientConfig;
import com.sinkedship.cerberus.commons.config.AbstractCerberusConfig;
import com.sinkedship.cerberus.commons.DataCenter;
import com.sinkedship.cerberus.commons.config.data_center.DataCenterConfig;

/**
 * Client configuration used to discover dedicated Thrift services.
 * <p>
 * This class contains two independent configuration:
 * <p>
 * a concrete implementation of {@link DataCenterConfig} and
 * a drift netty client config of {@link io.airlift.drift.transport.netty.client.DriftNettyClientConfig}
 *
 * @author Derrick Guan
 */
public final class CerberusClientConfig extends AbstractCerberusConfig {

    private DriftNettyClientConfig driftNettyClientConfig;

    public CerberusClientConfig(DataCenter dataCenter) {
        super(dataCenter);
        driftNettyClientConfig = new DriftNettyClientConfig();
    }

    public CerberusClientConfig(DataCenter dataCenter, DriftNettyClientConfig driftNettyClientConfig) {
        super(dataCenter);
        this.driftNettyClientConfig = driftNettyClientConfig;
    }

    public DriftNettyClientConfig getDriftNettyClientConfig() {
        return driftNettyClientConfig;
    }
}
