package com.sinkedship.cerberus.registry.etcd;

import com.sinkedship.cerberus.commons.config.data_center.EtcdConfig;
import com.sinkedship.cerberus.core.api.Provider;
import com.sinkedship.cerberus.core.api.Registrar;
import com.sinkedship.cerberus.core.api.Registry;
import io.etcd.jetcd.Client;

/**
 * @author Derrick Guan
 */
public class EtcdRegistry implements Registry {

    private final Registrar registrar;

    private final Provider provider;

    public EtcdRegistry(EtcdConfig config) {
        Client client = Client.builder().endpoints(config.getEndpoints()).build();
        registrar = new EtcdServiceRegistrar(config, client);
        provider = new EtcdServiceProvider(config, client);
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
        // Nothing to close
    }
}
