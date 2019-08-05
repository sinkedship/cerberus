package com.sinkedship.cerberus.sample.server;

import com.sinkedship.cerberus.bootstrap.CerberusServerBootstrap;
import com.sinkedship.cerberus.bootstrap.config.CerberusServerConfig;
import com.sinkedship.cerberus.commons.DataCenter;
import com.sinkedship.cerberus.commons.ServiceMetaData;
import com.sinkedship.cerberus.commons.utils.HostAndPortUtils;
import com.sinkedship.cerberus.sample.server.impl.CalculateService_Without_Explicit_Interface;
import com.sinkedship.cerberus.sample.server.impl.HelloServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Derrick Guan
 */
public class CerberusServerBootstrapSample {

    private static final Logger LOGGER = LogManager.getLogger(CerberusServerBootstrapSample.class);

    public static void main(String[] args) {
        // Use local data center which doesn't depend on any others middleware and friend for testing.
        DataCenter dataCenter = DataCenter.LOCAL;

        // Configs the server
        String host = HostAndPortUtils.getDefaultHost();
        CerberusServerConfig serverConfig = new CerberusServerConfig(dataCenter);
        serverConfig.getBootConfig().setHost(host);

        // Starts the server, containing boot-up and registration
        new CerberusServerBootstrap.Builder(dataCenter)
                .withServerConfig(serverConfig)
                .withService(new HelloServiceImpl())
                .withService(new CalculateService_Without_Explicit_Interface(),
                        new ServiceMetaData("com.sinkedship", "test", "calculate"))
                .build()
                .boot();

        LOGGER.info("Server starts up, listening on {}:{}", host,
                serverConfig.getBootConfig().getPort());
    }

}
