package com.sinkedship.cerberus.registry.zookeeper;

import com.sinkedship.cerberus.commons.config.data_center.ZookeeperConfig;
import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Provider;
import com.sinkedship.cerberus.core.api.Registrar;
import com.sinkedship.cerberus.core.api.Registry;
import com.sinkedship.cerberus.registry.zookeeper.serializer.GsonSerializer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * A implementation of {@link Registry}, using curator framework internally.
 *
 * @author Derrick Guan
 */
public class ZookeeperRegistry implements Registry {

    private static final Logger LOGGER = LogManager.getLogger(ZookeeperRegistry.class);

    private final Registrar registrar;

    private final Provider provider;

    private final CuratorFramework curatorClient;

    private final ServiceDiscovery<Service> curatorServiceDiscovery;

    public ZookeeperRegistry(ZookeeperConfig config) {
        // build a curator framework for later use of service discovery
        curatorClient = buildCurator(config);
        // build a curator service discovery and remember to start it before any actual usages
        curatorServiceDiscovery = buildCuratorServiceDiscovery(config, curatorClient);
        registrar = new ZookeeperServiceRegistrar(curatorServiceDiscovery);
        provider = new ZookeeperServiceProvider(
                curatorServiceDiscovery,
                new ZookeeperServiceDiscoverer(curatorServiceDiscovery));
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
    public void close() throws IOException {
        ((ZookeeperServiceProvider) provider).close();
        curatorServiceDiscovery.close();
        curatorClient.close();
    }

    private CuratorFramework buildCurator(ZookeeperConfig config) {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(config.getConnectString())
                .sessionTimeoutMs((int) config.getZkSessionTimeout())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();
        return client;
    }

    private ServiceDiscovery<Service> buildCuratorServiceDiscovery(
            ZookeeperConfig config, CuratorFramework curatorClient) {
        ServiceDiscovery<Service> serviceDiscovery = ServiceDiscoveryBuilder.builder(Service.class)
                .basePath(config.getBasePath())
                .serializer(new GsonSerializer<>(Service.class))
                .watchInstances(true)
                .client(curatorClient)
                .build();
        try {
            serviceDiscovery.start();
        } catch (Throwable e) {
            // Should never happen because we never provide any initial services.
            // However, as it happens we log it and can figure out what is going wrong?
            LOGGER.error("Curator service discovery starts with error", e);
            throw new RuntimeException("Curator service discovery starts with error", e);
        }
        return serviceDiscovery;
    }
}
