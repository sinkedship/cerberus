package com.sinkedship.cerberus.registry.zookeeper;

import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Discoverer;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Discoverer implementation for zookeeper data center, using curator framework.
 *
 * @author Derrick Guan
 */
public class ZookeeperServiceDiscoverer implements Discoverer {

    private static final Logger LOGGER = LogManager.getLogger(ZookeeperServiceDiscoverer.class);

    private final ServiceDiscovery<Service> serviceDiscovery;

    public ZookeeperServiceDiscoverer(ServiceDiscovery<Service> serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @Nonnull
    @Override
    public Map<String, List<Service>> findAll() {
        Map<String, List<Service>> ret = new HashMap<>();
        try {
            Collection<String> serviceNames = serviceDiscovery.queryForNames();
            for (String srvName : serviceNames) {
                ret.put(srvName, findAllByName(srvName));
            }
        } catch (Throwable e) {
            LOGGER.error("Curator discoverer raises error when tries to find all available services", e);
        }
        return ret;
    }

    @Nonnull
    @Override
    public List<Service> findAllByName(String name) {
        try {
            return doFindAllByName(name);
        } catch (Throwable e) {
            LOGGER.error("Curator discoverer raises error when tries to find service:{}", name, e);
        }
        return new ArrayList<>();
    }

    private List<Service> doFindAllByName(String name) throws Throwable {
        Collection<ServiceInstance<Service>> instances = serviceDiscovery.queryForInstances(name);
        List<Service> list = new ArrayList<>();
        instances.forEach(instance -> list.add(instance.getPayload()));
        return list;
    }
}
