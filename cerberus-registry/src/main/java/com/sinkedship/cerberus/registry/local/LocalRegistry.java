package com.sinkedship.cerberus.registry.local;

import com.sinkedship.cerberus.commons.config.data_center.LocalConfig;
import com.sinkedship.cerberus.core.api.Provider;
import com.sinkedship.cerberus.core.api.Registrar;
import com.sinkedship.cerberus.core.api.Registry;

/**
 * @author Derrick Guan
 */
public class LocalRegistry implements Registry {

    private final Registrar registrar;

    private final Provider provider;

    public LocalRegistry(LocalConfig config) {
        registrar = new LocalServiceRegistrar();
        provider = new LocalServiceProvider(config);
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
        // Left blank
    }
}
