package com.sinkedship.cerberus.registry.k8s;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.sinkedship.cerberus.commons.K8sServiceMetaData;
import com.sinkedship.cerberus.commons.config.data_center.K8sConfig;
import com.sinkedship.cerberus.commons.exception.CerberusException;
import com.sinkedship.cerberus.core.CerberusService;
import com.sinkedship.cerberus.core.Service;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.util.CallGeneratorParams;
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

    private final LoadingCache<K8sServiceMetaData, Service> cache;

    private static final class InternalCacheLoader extends CacheLoader<K8sServiceMetaData, Service> {

        final K8sServiceDiscoverer discoverer;
        final ListeningExecutorService executor = MoreExecutors.listeningDecorator(
                Executors.newSingleThreadExecutor());

        InternalCacheLoader(K8sServiceDiscoverer discoverer) {
            this.discoverer = discoverer;
        }

        @Override
        public Service load(K8sServiceMetaData metaData) throws Exception {
            LOGGER.debug("try to load k8s service by meta-data:{}", metaData);
            Optional<Service> svc = discoverer.resolveK8sService(metaData);
            return svc.orElseThrow(() -> new CerberusException("unable to resolve k8s svc"));
        }

        @Override
        public ListenableFuture<Service> reload(K8sServiceMetaData metaData, Service oldService) {
            checkNotNull(metaData);
            checkNotNull(oldService);
            return executor.submit(() -> {
                try {
                    Optional<Service> service = discoverer.resolveK8sService(metaData);
                    if (service.isPresent()) {
                        LOGGER.debug("reload service by meta-data:{}, reloaded svc:{}", metaData, service.get());
                        return service.get();
                    } else {
                        LOGGER.warn("reload service by meta-data:{}, returning empty svc", metaData);
                        return oldService;
                    }
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
                .maximumSize(config.getSvcCacheSize())
                .refreshAfterWrite(config.getSvcRefreshInterval(), TimeUnit.MILLISECONDS)
                .build(new InternalCacheLoader(this));
        if (config.isSvcWatch()) {
            watchSvc(config);
        }
    }

    private void watchSvc(K8sConfig config) {
        ApiClient apiClient = Config.fromToken(config.getBasePath(), config.getAuthToken(), config.verifySsl())
                .setReadTimeout(0);
        CoreV1Api api = new CoreV1Api(apiClient);
        SharedInformerFactory factory = new SharedInformerFactory(apiClient);
        SharedIndexInformer<V1Service> svcInformer = factory.sharedIndexInformerFor(
                (CallGeneratorParams params) -> api.listNamespacedServiceCall(config.getNamespace(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        params.resourceVersion,
                        params.timeoutSeconds,
                        params.watch,
                        null
                ),
                V1Service.class,
                V1ServiceList.class
        );
        svcInformer.addEventHandler(new ResourceEventHandler<V1Service>() {
            @Override
            public void onAdd(V1Service service) {
                if (service.getMetadata() != null && service.getSpec() != null &&
                        service.getSpec().getPorts() != null) {
                    String ip = service.getSpec().getClusterIP();
                    for (V1ServicePort port : service.getSpec().getPorts()) {
                        String svcName = service.getMetadata().getName();
                        String portName = port.getName();
                        K8sServiceMetaData metaData = new K8sServiceMetaData(svcName, portName);
                        // TODO(dguan) cache all the services?
                        cache.put(metaData, new CerberusService.Builder(Object.class)
                                .metaData(metaData).host(ip).port(port.getPort())
                                .build());
                    }
                }
            }

            @Override
            public void onUpdate(V1Service oldObj, V1Service newObj) {
            }

            @Override
            public void onDelete(V1Service service, boolean deletedFinalStateUnknown) {
                if (service.getMetadata() != null && service.getSpec() != null &&
                        service.getSpec().getPorts() != null) {
                    for (V1ServicePort port : service.getSpec().getPorts()) {
                        String svcName = service.getMetadata().getName();
                        String portName = port.getName();
                        K8sServiceMetaData metaData = new K8sServiceMetaData(svcName, portName);
                        cache.invalidate(metaData);
                    }
                }
            }
        });
        factory.startAllRegisteredInformers();
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
            return Optional.ofNullable(cache.get(metaData));
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
