package com.sinkedship.cerberus.registry.etcd;

import com.sinkedship.cerberus.core.Service;
import io.etcd.jetcd.Client;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author Derrick Guan
 */
class EtcdServiceKeeper implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(EtcdServiceKeeper.class);

    private final Client etcdClient;

    private ConcurrentHashMap<Long, Service> serviceMap = new ConcurrentHashMap<>();

    private final long keepInterval;

    private final Executor executor = Executors.newFixedThreadPool(10);

    EtcdServiceKeeper(Client etcdClient, long keepInterval) {
        this.etcdClient = etcdClient;
        this.keepInterval = keepInterval;
    }

    void keepService(Long leaseId, Service service) {
        serviceMap.putIfAbsent(leaseId, service);
    }

    Service removeService(Long leaseId) {
        return serviceMap.remove(leaseId);
    }

    Optional<Long> getLeaseId(Service service) {
        for (Map.Entry<Long, Service> entry : serviceMap.entrySet()) {
            if (entry.getValue().equals(service)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }


    @Override
    public void run() {
        while (true) {
            for (Map.Entry<Long, Service> entry : serviceMap.entrySet()) {
                long leaseId = entry.getKey();
                Service s = entry.getValue();
                executor.execute(() -> {
                    try {
                        etcdClient.getLeaseClient().keepAliveOnce(leaseId).get();
                        LOGGER.debug("Etcd keep alive succeed, lease id:{}", leaseId);
                    } catch (ExecutionException | InterruptedException e) {
                        LOGGER.error("Etcd keep alive fail, service:{} lease id:{}, due to:",
                                s.getIdentifier(), leaseId, e);
                        removeService(leaseId);
                    }
                });
            }
            try {
                Thread.sleep(keepInterval);
            } catch (InterruptedException e) {
                LOGGER.error("Consul service keeper interrupted", e);
                return;
            }
        }
    }
}
