package com.sinkedship.cerberus.registry.zookeeper;

import com.google.common.collect.Maps;
import com.sinkedship.cerberus.commons.ServiceMetaData;
import com.sinkedship.cerberus.commons.utils.CerberusStringUtils;
import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Discoverer;
import com.sinkedship.cerberus.core.api.Provider;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Provider implementation for zookeeper, using round-robin provider strategy as the default strategy.
 * <p>
 * Use curator {@link org.apache.curator.x.discovery.ServiceProvider} internally.
 *
 * @author Derrick Guan
 */
public class ZookeeperServiceProvider implements Provider {

    private static final Logger LOGGER = LogManager.getLogger(ZookeeperServiceProvider.class);

    private final ConcurrentMap<String, ServiceProvider<Service>> providerMap;

    private final ConcurrentMap<Class<?>, String> clzNameMap;

    private final ServiceDiscovery<Service> curatorServiceDiscovery;

    private final Discoverer discoverer;

    public ZookeeperServiceProvider(ServiceDiscovery<Service> curatorServiceDiscovery, Discoverer discoverer) {
        providerMap = Maps.newConcurrentMap();
        clzNameMap = Maps.newConcurrentMap();
        this.curatorServiceDiscovery = curatorServiceDiscovery;
        this.discoverer = discoverer;
    }

    @Override
    public Optional<Service> get(ServiceMetaData metaData) {
        try {
            return getWithCuratorServiceProvider(metaData.getServiceIdentifier());
        } catch (Throwable t) {
            LOGGER.error("Cannot resolve service instance from zookeeper with service identifier:{}, due to:",
                    metaData.getServiceIdentifier(), t);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Service> get(Class<?> targetClass) {
        return get(targetClass, CerberusStringUtils.DEFAULT_ASYNC_SUFFIX_SUPPLIER);
    }

    @Override
    public Optional<Service> get(Class<?> targetClass, Supplier<String> asyncSuffixSupplier) {
        // find service name associated with target class
        String serviceName = null;
        if (clzNameMap.containsKey(targetClass)) {
            serviceName = clzNameMap.get(targetClass);
        } else {
            Map<String, List<Service>> allServicesMap = discoverer.findAll();
            for (Map.Entry<String, List<Service>> entry : allServicesMap.entrySet()) {
                if (!entry.getValue().isEmpty() &&
                        entry.getValue().get(0).getIdentifier().equalsIgnoreCase(
                                CerberusStringUtils.stripAsyncSuffix(targetClass.getCanonicalName(),
                                        asyncSuffixSupplier))) {
                    serviceName = entry.getKey();
                    clzNameMap.putIfAbsent(targetClass, serviceName);
                    break;
                }
            }
        }
        if (StringUtils.isBlank(serviceName)) {
            return Optional.empty();
        } else {
            try {
                return getWithCuratorServiceProvider(serviceName);
            } catch (Throwable t) {
                LOGGER.error("Cannot resolve service instance from zookeeper with target class:{}, due to:",
                        targetClass.getCanonicalName(), t);
                return Optional.empty();
            }
        }
    }

    private Optional<Service> getWithCuratorServiceProvider(String serviceName) throws Exception {
        ServiceProvider<Service> curatorProvider = providerMap.get(serviceName);
        if (curatorProvider == null) {
            synchronized (providerMap) {
                curatorProvider = providerMap.get(serviceName);
                if (curatorProvider == null) {
                    curatorProvider = curatorServiceDiscovery.serviceProviderBuilder()
                            .serviceName(serviceName)
                            .providerStrategy(new RoundRobinStrategy<>())
                            .build();
                    ServiceProvider<Service> former = providerMap.putIfAbsent(serviceName, curatorProvider);
                    if (former != null) {
                        // should probably not happen, but it does,
                        // let's close it to prevent memory leaks.
                        former.close();
                    }
                    curatorProvider.start();
                }
            }
        }
        ServiceInstance<Service> instance = curatorProvider.getInstance();
        return instance == null ? Optional.empty() : Optional.of(instance.getPayload());
    }

    protected void close() {
        for (ServiceProvider<Service> provider : providerMap.values()) {
            try {
                provider.close();
            } catch (IOException e) {
                LOGGER.error("Try to close curator service provider with error:", e);
            }
        }
    }
}
