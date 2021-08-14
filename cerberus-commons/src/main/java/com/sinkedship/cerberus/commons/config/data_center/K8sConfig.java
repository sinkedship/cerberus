package com.sinkedship.cerberus.commons.config.data_center;

import com.google.common.base.Preconditions;
import com.sinkedship.cerberus.commons.DataCenter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class K8sConfig extends DataCenterConfig {

    // Env from within kubernetes
    private static final String DEFAULT_API_SERVER_HOST = System.getenv("KUBERNETES_SERVICE_HOST");
    private static final String DEFAULT_API_SERVER_PORT = System.getenv("KUBERNETES_SERVICE_PORT");

    private String namespace = "default";
    private String apiServerHost = DEFAULT_API_SERVER_HOST;
    private int apiServerPort = StringUtils.isBlank(DEFAULT_API_SERVER_PORT) ? 443 : Integer.parseInt(DEFAULT_API_SERVER_PORT);
    private boolean verifySsl = false;
    private String authToken;
    private long svcRefreshInterval = 30 * 1000;
    private int svcCacheSize = 100;
    private boolean svcWatch = false;

    private boolean debugWithNodePort = false;
    private String debugNodeHost = "";

    public K8sConfig() {
        super(DataCenter.K8S);
    }

    public K8sConfig setNamespace(String ns) {
        Preconditions.checkNotNull(ns, "k8s namespace cannot be null");
        this.namespace = ns;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public K8sConfig setApiServerHost(String host) {
        Preconditions.checkNotNull(host, "k8s api server host cannot be null");
        this.apiServerHost = host;
        return this;
    }

    public K8sConfig setApiServerPort(int port) {
        Preconditions.checkArgument(port >= 0 && port <= 65536,
                "k8s api server port cannot be out of range of [0, 65536]");
        this.apiServerPort = port;
        return this;
    }

    public String getBasePath() {
        return String.format("https://%s:%d", apiServerHost, apiServerPort);
    }

    public K8sConfig setVerifySsl(boolean verifySsl) {
        this.verifySsl = verifySsl;
        return this;
    }

    public boolean verifySsl() {
        return verifySsl;
    }

    public K8sConfig setAuthToken(String token) {
        Preconditions.checkNotNull(token, "k8s api server auth token cannot be null");
        this.authToken = token;
        return this;
    }

    public String getAuthToken() {
        if (StringUtils.isNotBlank(authToken)) {
            return authToken;
        }
        // Try to load token from cluster token
        String tokenFilePath = "/var/run/secrets/kubernetes.io/serviceaccount/token";
        try {
            return new String(Files.readAllBytes(Paths.get(tokenFilePath)), Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public K8sConfig setDebugWithNodePort(boolean debugWithNodePort) {
        this.debugWithNodePort = debugWithNodePort;
        return this;
    }

    public boolean debugWithNodePort() {
        return debugWithNodePort;
    }

    public K8sConfig setDebugNodeHost(String host) {
        Preconditions.checkNotNull(host, "k8s debug node host cannot be null");
        this.debugNodeHost = host;
        return this;
    }

    public String getDebugNodeHost() {
        return debugNodeHost;
    }

    public long getSvcRefreshInterval() {
        return svcRefreshInterval;
    }

    public void setSvcRefreshInterval(long svcRefreshInterval) {
        this.svcRefreshInterval = svcRefreshInterval;
    }

    public int getSvcCacheSize() {
        return svcCacheSize;
    }

    public void setSvcCacheSize(int svcCacheSize) {
        this.svcCacheSize = svcCacheSize;
    }

    public boolean isSvcWatch() {
        return svcWatch;
    }

    public void setSvcWatch(boolean svcWatch) {
        this.svcWatch = svcWatch;
    }
}
