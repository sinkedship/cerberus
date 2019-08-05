package com.sinkedship.cerberus.registry.etcd;

import com.sinkedship.cerberus.commons.config.data_center.EtcdConfig;
import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Registrar;
import com.google.gson.Gson;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.common.exception.ClosedClientException;
import io.etcd.jetcd.options.PutOption;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * @author Derrick Guan
 */
public class EtcdServiceRegistrar implements Registrar {

    private static final Logger LOGGER = LogManager.getLogger(EtcdServiceRegistrar.class);

    private final Client etcdClient;

    private final Gson gson = new Gson();

    private final String keyPrefix;

    private final long serviceTTL;

    private final EtcdServiceKeeper serviceKeeper;

    public EtcdServiceRegistrar(EtcdConfig config, Client etcdClient) {
        this.etcdClient = etcdClient;
        this.keyPrefix = config.getKeyPrefix();
        this.serviceTTL = config.getServiceTTL() / 1000;
        this.serviceKeeper = new EtcdServiceKeeper(etcdClient, config.getServiceKeepInterval());
        Thread keeperThread = new Thread(serviceKeeper);
        keeperThread.setDaemon(true);
        keeperThread.start();
    }

    @Override
    public <S extends Service> boolean register(S service) {
        if (StringUtils.isBlank(service.getIdentifier())) {
            LOGGER.error("Aborting registration, service:{} with an empty " +
                    "identifier cannot be registered to Etcd", service.getIdentifier());
            return false;
        }
        if (StringUtils.isBlank(service.getId())) {
            LOGGER.error("Aborting registration, service:{} with an empty " +
                    "id cannot be registered to Etcd", service.getIdentifier());
            return false;
        }

        try {
            Lease lease = etcdClient.getLeaseClient();
            long leaseId = lease.grant(serviceTTL).get().getID();
            PutOption option = PutOption.newBuilder().withLeaseId(leaseId).build();
            ByteSequence k = ByteSequence.from(getKey(service), StandardCharsets.UTF_8);
            ByteSequence v = ByteSequence.from(getServiceJson(service), StandardCharsets.UTF_8);
            // make it synchronous by invoking CompletableFuture's get
            etcdClient.getKVClient().put(k, v, option).get();
            LOGGER.debug("Register service:{}, id:{} to Etcd successfully",
                    service.getIdentifier(), service.getId());
            serviceKeeper.keepService(leaseId, service);
            return true;
        } catch (ClosedClientException e) {
            LOGGER.error("Cannot keep service:{}, id:{} alive in Etcd due to:",
                    service.getIdentifier(), service.getId(), e);
            // Cannot keep service alive, remove and report it
            unregister(service);
            return false;
        } catch (Exception e) {
            LOGGER.error("Unable to register service:{}, id:{} to Etcd due to:",
                    service.getIdentifier(), service.getId(), e);
            return false;
        }
    }

    @Override
    public <S extends Service> boolean register(S service, long timeout) {
        throw new UnsupportedOperationException(
                "EtcdServiceRegistrar does not support registering service with timeout by now");
    }

    @Override
    public <S extends Service> boolean unregister(S service) {
        try {
            Optional<Long> leaseId = serviceKeeper.getLeaseId(service);
            if (leaseId.isPresent()) {
                etcdClient.getLeaseClient().revoke(leaseId.get()).get();
                serviceKeeper.removeService(leaseId.get());
                return true;
            } else {
                LOGGER.warn("Cannot unregister service:{}, id:{} because cannot find associate lease id in Etcd",
                        service.getIdentifier(), service.getId());
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Cannot unregister service:{}, id:{} due to",
                    service.getIdentifier(), service.getId(), e);
            return false;
        }
    }

    private String getServiceJson(Service s) {
        return gson.toJson(s);
    }

    private String getKey(Service s) {
        return EtcdUtils.getInstanceKey(keyPrefix, s.getIdentifier(), s.getId());
    }
}