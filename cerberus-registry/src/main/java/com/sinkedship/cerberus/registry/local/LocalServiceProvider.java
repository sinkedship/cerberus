package com.sinkedship.cerberus.registry.local;

import com.sinkedship.cerberus.commons.ServiceMetaData;
import com.sinkedship.cerberus.commons.config.data_center.LocalConfig;
import com.sinkedship.cerberus.core.CerberusService;
import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Provider;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Derrick Guan
 */
public class LocalServiceProvider implements Provider {

    private final CerberusService service;

    LocalServiceProvider(LocalConfig config) {
        service = new CerberusService.Builder(Object.class)
                .host(config.getConnectHost())
                .port(config.getConnectPort())
                .build();
    }

    @Override
    public Optional<Service> get(ServiceMetaData metaData) {
        return Optional.of(service);
    }

    @Override
    public Optional<Service> get(Class<?> targetClass) {
        return Optional.of(service);
    }

    @Override
    public Optional<Service> get(Class<?> targetClass, Supplier<String> asyncSuffixSupplier) {
        return Optional.of(service);
    }
}
