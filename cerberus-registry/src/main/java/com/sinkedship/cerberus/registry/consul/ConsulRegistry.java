package com.sinkedship.cerberus.registry.consul;

import com.sinkedship.cerberus.commons.config.data_center.ConsulConfig;
import com.sinkedship.cerberus.core.api.Provider;
import com.sinkedship.cerberus.core.api.Registrar;
import com.sinkedship.cerberus.core.api.Registry;
import com.ecwid.consul.v1.ConsulClient;

/**
 * @author Derrick Guan
 */
public class ConsulRegistry implements Registry {

    private final Registrar registrar;

    private final Provider provider;

    public ConsulRegistry(ConsulConfig config) {
        ConsulClient client = new ConsulClient(config.getHost(), config.getPort());
        registrar = new ConsulServiceRegistrar(client);
        provider = new ConsulServiceProvider(client);
    }

    @Override
    public Provider provider() {
        return provider;
    }

    @Override
    public Registrar registrar() {
        return registrar;
    }

    @Override
    public void close() {
        // Nothing to close;
    }
}
