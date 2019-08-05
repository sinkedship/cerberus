package com.sinkedship.cerberus.registry.consul;

import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Discoverer;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Derrick Guan
 */
public class ConsulServiceDiscoverer implements Discoverer {

    private final static Logger LOGGER = LogManager.getLogger(ConsulServiceDiscoverer.class);

    private final ConsulClient consulClient;

    private final LoadingCache<String, List<Service>> cache;

    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    public ConsulServiceDiscoverer(ConsulClient consulClient) {
        Preconditions.checkNotNull(consulClient, "Consul client cannot be null");
        this.consulClient = consulClient;
        cache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .refreshAfterWrite(5, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<Service>>() {
                    @Override
                    public List<Service> load(String key) {
                        return doQueryConsul(key);
                    }

                    @Override
                    public ListenableFuture<List<Service>> reload(String key, List<Service> oldValue) {
                        ListenableFutureTask<List<Service>> task = ListenableFutureTask.create(
                                () -> doQueryConsul(key));
                        executor.execute(task);
                        return task;
                    }
                });

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

    private List<Service> doQueryConsul(String name) {
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setPassing(true)
                .setQueryParams(QueryParams.DEFAULT)
                .build();
        Response<List<HealthService>> response = consulClient.getHealthServices(name, request);
        List<HealthService> healthServices = response.getValue();

        List<Service> ret = new ArrayList<>();
        for (HealthService healthService : healthServices) {
            HealthService.Service hs = healthService.getService();
            ret.add(new Service(name, name, hs.getId(), hs.getAddress(), hs.getPort()));
        }
        return ret;
    }
}
