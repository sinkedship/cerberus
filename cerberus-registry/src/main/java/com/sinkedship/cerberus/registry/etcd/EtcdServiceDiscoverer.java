package com.sinkedship.cerberus.registry.etcd;

import com.sinkedship.cerberus.commons.config.data_center.EtcdConfig;
import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Discoverer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @author Derrick Guan
 */
public class EtcdServiceDiscoverer implements Discoverer {

    private static final Logger LOGGER = LogManager.getLogger(EtcdServiceDiscoverer.class);

    private final Client etcdClient;

    private final String keyPrefix;

    private final LoadingCache<String, List<Service>> cache;

    private final Gson gson = new Gson();

    public EtcdServiceDiscoverer(EtcdConfig config, Client client) {
        this.etcdClient = client;
        this.keyPrefix = config.getKeyPrefix();
        cache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .build(new CacheLoader<String, List<Service>>() {
                    @Override
                    public List<Service> load(String key) {
                        return doQueryEtcd(key);
                    }
                });
        watch();
    }

    @Nonnull
    @Override
    public List<Service> findAllByName(String name) {
        try {
            return cache.get(name);
        } catch (Throwable t) {
            LOGGER.error("Discover service:{} from Cache with error", name, t);
            return Collections.emptyList();
        }
    }

    private List<Service> doQueryEtcd(String serviceName) {
        ByteSequence k = ByteSequence.from(getKey(serviceName), StandardCharsets.UTF_8);
        GetOption getOption = GetOption.newBuilder().withPrefix(k).build();
        List<Service> ret = new ArrayList<>();
        try {
            GetResponse response = etcdClient.getKVClient().get(k, getOption).get();
            List<KeyValue> kvs = response.getKvs();
            for (KeyValue kv : kvs) {
                ByteSequence v = kv.getValue();
                Optional<Service> service = fromJson(v.getBytes());
                service.ifPresent(ret::add);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.warn("Cannot get service:{} from Etcd due to", serviceName, e);
            return Collections.emptyList();
        }
        return ret;
    }

    private void watch() {
        ByteSequence k = ByteSequence.from(keyPrefix, StandardCharsets.UTF_8);
        WatchOption watchOption = WatchOption.newBuilder()
                .withPrefix(k)
                .withPrevKV(true)
                .withProgressNotify(true)
                .build();
        etcdClient.getWatchClient().watch(k, watchOption,
                response -> response.getEvents().forEach(this::processEvent),
                throwable -> LOGGER.error("Watch etcd with error", throwable));
    }

    private String getKey(String serviceName) {
        return EtcdUtils.getServiceKey(keyPrefix, serviceName);
    }

    private Optional<Service> fromJson(byte[] raw) {
        String json = new String(raw);
        try {
            return Optional.of(gson.fromJson(json, Service.class));
        } catch (Throwable t) {
            LOGGER.error("Cannot deserialize Json string:{} from Etcd watch event to service due to", json, t);
            return Optional.empty();
        }
    }

    private void processEvent(WatchEvent event) {
        WatchEvent.EventType eventType = event.getEventType();
        KeyValue kv = event.getKeyValue();
        String key = new String(kv.getKey().getBytes());
        Optional<String> svcOpt = extractServiceNameFromKey(key);
        if (!svcOpt.isPresent()) {
            LOGGER.warn("Cannot extract service name from watch event:{} and key:{}", eventType.name(), key);
            return;
        }
        String serviceName = svcOpt.get();
        switch (eventType) {
            case PUT: {
                Optional<Service> value = fromJson(kv.getValue().getBytes());
                value.ifPresent(service -> processPutEvent(key, serviceName, service));
                break;
            }
            case DELETE: {
                Optional<Service> value = fromJson(event.getPrevKV().getValue().getBytes());
                value.ifPresent(service -> processDeleteEvent(key, serviceName, service));
                break;
            }
        }
    }

    private void processPutEvent(String key, String serviceName, Service service) {
        List<Service> serviceList = cache.getIfPresent(serviceName);
        if (serviceList == null) {
            serviceList = new ArrayList<>();
        }
        serviceList.add(service);
        cache.put(serviceName, serviceList);
        LOGGER.debug("Put new service instance:{} to cache by key:{} successfully", service, key);
    }

    private void processDeleteEvent(String key, String serviceName, Service prevService) {
        List<Service> serviceList = cache.getIfPresent(serviceName);
        if (serviceList == null || serviceList.isEmpty() || !serviceList.contains(prevService)) {
            LOGGER.warn("Cannot find any service instances from cache by key:{}", key);
            return;
        }
        if (serviceList.remove(prevService)) {
            LOGGER.debug("Remove service instance:{} from cache by key:{} successfully", prevService, key);
        } else {
            LOGGER.warn("Cannot remove service instance:{} from cache by key:{}", prevService, key);
        }
    }

    private Optional<String> extractServiceNameFromKey(String key) {
        String[] elements = key.split("/");
        if (elements.length < 2) {
            return Optional.empty();
        } else {
            return Optional.of(elements[elements.length - 2]);
        }
    }
}
