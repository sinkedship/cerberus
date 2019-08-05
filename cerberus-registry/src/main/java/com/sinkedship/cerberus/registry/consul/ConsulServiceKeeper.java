package com.sinkedship.cerberus.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Derrick Guan
 */
class ConsulServiceKeeper implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(ConsulServiceKeeper.class);

    private final ConsulClient consulClient;

    private List<String> serviceIdList = new CopyOnWriteArrayList<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    ConsulServiceKeeper(ConsulClient consulClient) {
        this.consulClient = consulClient;
    }

    void addServiceId(String serviceId) {
        serviceIdList.add(serviceId);
    }

    void removeServiceId(String serviceId) {
        serviceIdList.remove(serviceId);
    }

    @Override
    public void run() {
        while (true) {
            for (String s : serviceIdList) {
                executor.submit(() -> {
                    try {
                        consulClient.agentCheckPass("service:" + s);
                    } catch (Throwable t) {
                        LOGGER.error("Check service with id:{} failed, going to remove it from check-list.", s, t);
                        serviceIdList.remove(s);
                    }
                });
            }
            try {
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                LOGGER.error("Consul service keeper interrupted", e);
                return;
            }
        }
    }
}
