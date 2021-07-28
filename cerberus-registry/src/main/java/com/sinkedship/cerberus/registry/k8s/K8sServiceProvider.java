package com.sinkedship.cerberus.registry.k8s;

import com.sinkedship.cerberus.commons.K8sServiceMetaData;
import com.sinkedship.cerberus.commons.ServiceMetaData;
import com.sinkedship.cerberus.commons.config.data_center.K8sConfig;
import com.sinkedship.cerberus.core.CerberusService;
import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Provider;
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
import java.util.function.Supplier;

public class K8sServiceProvider implements Provider {

    private static final Logger LOGGER = LogManager.getLogger(K8sServiceProvider.class);

    private final CoreV1Api coreV1Api;

    private final String ns;

    private boolean debugWithNodePort;
    private String debugNodeHost = "";

    public K8sServiceProvider(K8sConfig config) {
        this.ns = config.getNamespace();
        this.debugWithNodePort = config.debugWithNodePort();
        this.debugNodeHost = config.getDebugNodeHost();
        ApiClient apiClient = Config.fromToken(config.getBasePath(), config.getAuthToken(), config.verifySsl());
        Configuration.setDefaultApiClient(apiClient);
        coreV1Api = new CoreV1Api();
    }

    @Override
    public Optional<Service> get(ServiceMetaData metaData) {
        try {
            K8sServiceMetaData k8sSvcMetaData = (K8sServiceMetaData) metaData;

            Service svc = null;
            V1Service v1Service = coreV1Api.readNamespacedService(k8sSvcMetaData.getServiceIdentifier(), ns,
                    null, null, null);
            if (v1Service.getSpec() == null ||
                    v1Service.getSpec().getPorts() == null ||
                    v1Service.getSpec().getPorts().isEmpty()) {
                return Optional.empty();
            }

            // If debugWithNodePort flag set to true, parse svc with node-port
            String ip;
            Integer port = null;
            if (debugWithNodePort) {
                ip = debugNodeHost;
                for (V1ServicePort v1ServicePort : v1Service.getSpec().getPorts()) {
                    if (v1ServicePort.getName() != null && v1ServicePort.getName().equalsIgnoreCase(
                            k8sSvcMetaData.getServicePortName())) {
                        port = v1ServicePort.getNodePort();
                        break;
                    }
                }
            } else {
                // Normal resolving case
                ip = v1Service.getSpec().getClusterIP();
                for (V1ServicePort v1ServicePort : v1Service.getSpec().getPorts()) {
                    if (v1ServicePort.getName() != null && v1ServicePort.getName().equalsIgnoreCase(
                            k8sSvcMetaData.getServicePortName())) {
                        port = v1ServicePort.getPort();
                        break;
                    }
                }
            }
            if (port != null && StringUtils.isNotBlank(ip)) {
                svc = new CerberusService.Builder(Object.class)
                        .metaData(k8sSvcMetaData).host(ip).port(port)
                        .build();
                LOGGER.debug("resolve k8s service with meta data:{}, ip:{}, port:{}",
                        k8sSvcMetaData, ip, port);
            }
            return Optional.ofNullable(svc);
        } catch (ClassCastException e) {
            LOGGER.warn("Cannot cast service metadata {} to k8s metadata", metaData);
        } catch (ApiException e) {
            LOGGER.warn("Cannot list service from k8s", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Service> get(Class<?> targetClass) {
        throw new UnsupportedOperationException("K8s service provider does not support resolving service by target class");
    }

    @Override
    public Optional<Service> get(Class<?> targetClass, Supplier<String> asyncSuffixSupplier) {
        throw new UnsupportedOperationException("K8s service provider does not support resolving service by target class");
    }
}
