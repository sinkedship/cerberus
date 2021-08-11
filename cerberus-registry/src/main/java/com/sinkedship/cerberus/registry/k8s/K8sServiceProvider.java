package com.sinkedship.cerberus.registry.k8s;

import com.sinkedship.cerberus.commons.K8sServiceMetaData;
import com.sinkedship.cerberus.commons.ServiceMetaData;
import com.sinkedship.cerberus.commons.config.data_center.K8sConfig;
import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.Supplier;

public class K8sServiceProvider implements Provider {

    private static final Logger LOGGER = LogManager.getLogger(K8sServiceProvider.class);

    private final boolean debugWithNodePort;

    private final K8sServiceDiscoverer discoverer;

    public K8sServiceProvider(K8sConfig config) {
        this.debugWithNodePort = config.debugWithNodePort();
        discoverer = new K8sServiceDiscoverer(config);
    }

    @Override
    public Optional<Service> get(ServiceMetaData metaData) {
        try {
            K8sServiceMetaData k8sSvcMetaData = (K8sServiceMetaData) metaData;
            // If debugWithNodePort flag set to true, parse svc with node-port
            if (debugWithNodePort) {
                return discoverer.findK8sNodePortService(k8sSvcMetaData);
            } else {
                // normal resolving case
                return discoverer.findK8sService(k8sSvcMetaData);
            }
        } catch (ClassCastException e) {
            LOGGER.warn("Cannot cast service metadata {} to k8s metadata", metaData);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Service> get(Class<?> targetClass) {
        throw new UnsupportedOperationException("K8s service provider does not support resolving service by target class");
    }

    @Override
    public Optional<Service> get(Class<?> targetClass, Supplier<String> asyncSuffixSupplier) {
        throw new UnsupportedOperationException("K8s service provider does not support resolving service by target class");
    }
}
