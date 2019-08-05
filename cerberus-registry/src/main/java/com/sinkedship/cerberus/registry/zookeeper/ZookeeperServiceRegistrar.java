package com.sinkedship.cerberus.registry.zookeeper;

import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Registrar;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Registrar implementation for zookeeper data center, using curator framework.
 *
 * @author Derrick Guan
 */
public class ZookeeperServiceRegistrar implements Registrar {

    private static final Logger LOGGER = LogManager.getLogger(ZookeeperServiceRegistrar.class);

    private final ServiceDiscovery<Service> serviceDiscovery;

    public ZookeeperServiceRegistrar(ServiceDiscovery<Service> serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @Override
    public <S extends Service> boolean register(S service) {
        if (StringUtils.isBlank(service.getId())) {
            LOGGER.error("Aborting registration, service:{} with an empty " +
                    "identifier cannot be registered to zookeeper", service.getIdentifier());
            return false;
        }
        try {
            serviceDiscovery.registerService(buildServiceInstance(service));
            LOGGER.debug("Register service:{}, id:{} to zookeeper successfully",
                    service.getIdentifier(), service.getId());
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to register service:{}, id:{} to zookeeper due to:",
                    service.getIdentifier(), service.getId(), e);
            return false;
        }
    }

    @Override
    public <S extends Service> boolean register(S service, long timeout) {
        // Temporarily not supported
        throw new UnsupportedOperationException(
                "ZookeeperServiceRegistrar does not support registering service with timeout by now");
    }

    @Override
    public <S extends Service> boolean unregister(S service) {
        if (StringUtils.isBlank(service.getId())) {
            LOGGER.error("Cannot unregister a service:{} with an empty identifier",
                    service.getIdentifier());
            return false;
        }
        ServiceInstance<Service> instance = buildServiceInstance(service);
        try {
            serviceDiscovery.unregisterService(instance);
            LOGGER.debug("Unregister service:{}, id:{} from zookeeper successfully",
                    service.getIdentifier(), service.getId());
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to unregister service:{}, id:{} from zookeeper due to:",
                    service.getIdentifier(), service.getId(), e);
            return false;
        }
    }

    private ServiceInstance<Service> buildServiceInstance(Service service) {

        // Not going to use ServiceInstanceBuilder to build an actual instance,
        // because every time you call the builder method, it does some pre-loading
        // information which is waste of time, in my opinion, at least.
        // So here we use the verbose constructor to build an instance though it may
        // be a litter cumbersome.

        return new ServiceInstance<>(
                service.getIdentifier(),
                service.getId(),
                service.getHost(),
                service.getPort(),
                null,
                service,
                System.currentTimeMillis(),
                ServiceType.DYNAMIC,
                null,
                true
        );
    }
}
