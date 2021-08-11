package com.sinkedship.cerberus.registry.k8s;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.sinkedship.cerberus.commons.K8sServiceMetaData;
import com.sinkedship.cerberus.commons.config.data_center.K8sConfig;
import com.sinkedship.cerberus.core.CerberusService;
import com.sinkedship.cerberus.core.Service;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.util.Config;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

class K8sServiceDiscoverer {
    private static final Logger LOGGER = LogManager.getLogger(K8sServiceDiscoverer.class);

    private final CoreV1Api coreV1Api;

    private final String ns;

    private final String debugNodeHost;

    private final LoadingCache<K8sServiceMetaData, Optional<Service>> cache;

    private static final class InternalCacheLoader extends CacheLoader<K8sServiceMetaData, Optional<Service>> {

        final K8sServiceDiscoverer discoverer;
        final ListeningExecutorService executor = MoreExecutors.listeningDecorator(
                Executors.newSingleThreadExecutor());

        InternalCacheLoader(K8sServiceDiscoverer discoverer) {
            this.discoverer = discoverer;
        }

        @Override
        public Optional<Service> load(K8sServiceMetaData metaData) {
            try {
                return discoverer.resolveK8sService(metaData);
            } catch (ApiException e) {
                LOGGER.warn("Cannot list service from k8s", e);
                return Optional.empty();
            }
        }

        @Override
        public ListenableFuture<Optional<Service>> reload(K8sServiceMetaData metaData, Optional<Service> oldService) {
            checkNotNull(metaData);
            checkNotNull(oldService);
            return executor.submit(() -> {
                try {
                    Optional<Service> service = discoverer.resolveK8sService(metaData);
                    if (service.isPresent()) {
                        LOGGER.debug("reload service by meta-data:{}, reloaded svc:{}", metaData, service.get());
                    } else {
                        LOGGER.warn("reload service by meta-data:{}, returning empty svc", metaData);
                    }
                    return service;
                } catch (Exception e) {
                    LOGGER.warn("cache reload with error, returning old service, k8s svc meta-data:{}", metaData);
                    return oldService;
                }
            });
        }
    }

    K8sServiceDiscoverer(K8sConfig config) {
        this.ns = config.getNamespace();
        this.debugNodeHost = config.getDebugNodeHost();
        ApiClient apiClient = Config.fromToken(config.getBasePath(), config.getAuthToken(), config.verifySsl());
        Configuration.setDefaultApiClient(apiClient);
        coreV1Api = new CoreV1Api();
        cache = CacheBuilder.newBuilder()
                .refreshAfterWrite(config.getSvcRefreshInterval(), TimeUnit.MILLISECONDS)
                .build(new InternalCacheLoader(this));
    }

    private Optional<Service> resolveK8sService(K8sServiceMetaData metaData) throws ApiException {
        V1Service v1Service = coreV1Api.readNamespacedService(metaData.getServiceIdentifier(), ns,
                null, null, null);
        if (v1Service.getSpec() == null ||
                v1Service.getSpec().getPorts() == null ||
                v1Service.getSpec().getPorts().isEmpty()) {
            return Optional.empty();
        }
        String ip = v1Service.getSpec().getClusterIP();
        Integer port = null;
        for (V1ServicePort v1ServicePort : v1Service.getSpec().getPorts()) {
            if (v1ServicePort.getName() != null && v1ServicePort.getName().equalsIgnoreCase(
                    metaData.getServicePortName())) {
                port = v1ServicePort.getPort();
                break;
            }
        }
        if (port != null && StringUtils.isNotBlank(ip)) {
            Service svc = new CerberusService.Builder(Object.class)
                    .metaData(metaData).host(ip).port(port)
                    .build();
            LOGGER.debug("resolve k8s service with meta data:{}, returning ip:{}, port:{}",
                    metaData, ip, port);
            return Optional.of(svc);
        } else {
            return Optional.empty();
        }
    }

    Optional<Service> findK8sService(K8sServiceMetaData metaData) {
        try {
            return cache.get(metaData);
        } catch (Throwable t) {
            LOGGER.warn("resolve K8S service by meta data:{} with error", metaData, t);
            return Optional.empty();
        }
    }

    Optional<Service> findK8sNodePortService(K8sServiceMetaData metaData) {
        try {
            V1Service v1Service = coreV1Api.readNamespacedService(metaData.getServiceIdentifier(), ns,
                    null, null, null);
            if (v1Service.getSpec() == null ||
                    v1Service.getSpec().getPorts() == null ||
                    v1Service.getSpec().getPorts().isEmpty()) {
                return Optional.empty();
            }
            String ip = debugNodeHost;
            Integer nodePort = null;
            for (V1ServicePort v1ServicePort : v1Service.getSpec().getPorts()) {
                if (v1ServicePort.getName() != null && v1ServicePort.getName().equalsIgnoreCase(
                        metaData.getServicePortName())) {
                    nodePort = v1ServicePort.getNodePort();
                    break;
                }
            }
            if (nodePort != null) {
                Service svc = new CerberusService.Builder(Object.class)
                        .metaData(metaData).host(ip).port(nodePort)
                        .build();
                LOGGER.debug("resolve k8s node-port service by meta data:{}, returning ip:{}, node-port:{}",
                        metaData, ip, nodePort);
                return Optional.of(svc);
            } else {
                LOGGER.debug("fail to resolve k8s node-port service by meta data:{}", metaData);
                return Optional.empty();
            }
        } catch (ApiException e) {
            LOGGER.warn("resolve k8s node-port service by meta data:{} with error", metaData, e);
            return Optional.empty();
        }
    }
}
