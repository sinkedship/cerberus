package com.sinkedship.cerberus.bootstrap.config;

import com.sinkedship.cerberus.commons.config.AbstractCerberusConfig;
import com.sinkedship.cerberus.commons.DataCenter;
import com.sinkedship.cerberus.commons.config.data_center.DataCenterConfig;

/**
 * Server configuration used to boot and register delegated service.
 * <p>
 * This class contains two independent configuration:
 * <p>
 * a concrete implementation of {@link DataCenterConfig} and
 * a boot related configuration of {@link CerberusServerBootConfig}.
 *
 * @author Derrick Guan
 */
public final class CerberusServerConfig extends AbstractCerberusConfig {

    private final CerberusServerBootConfig cerberusServerBootConfig;

    public CerberusServerConfig(DataCenter dataCenter) {
        super(dataCenter);
        cerberusServerBootConfig = new CerberusServerBootConfig();
    }

    public CerberusServerBootConfig getBootConfig() {
        return cerberusServerBootConfig;
    }
}
