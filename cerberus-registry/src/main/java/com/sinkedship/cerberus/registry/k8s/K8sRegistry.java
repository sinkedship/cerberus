package com.sinkedship.cerberus.registry.k8s;

import com.sinkedship.cerberus.commons.config.data_center.K8sConfig;
import com.sinkedship.cerberus.core.api.Provider;
import com.sinkedship.cerberus.core.api.Registrar;
import com.sinkedship.cerberus.core.api.Registry;

public class K8sRegistry implements Registry {

    private final Registrar registrar;

    private final Provider provider;

    public K8sRegistry(K8sConfig k8sConfig) {
        registrar = new K8sServiceRegistrar();
        provider = new K8sServiceProvider(k8sConfig);
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
