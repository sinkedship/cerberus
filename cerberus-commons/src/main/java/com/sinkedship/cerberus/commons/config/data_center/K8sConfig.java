package com.sinkedship.cerberus.commons.config.data_center;

import com.google.common.base.Preconditions;
import com.sinkedship.cerberus.commons.DataCenter;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class K8sConfig extends DataCenterConfig {

    // Env from within kubernetes
    private static final String DEFAULT_API_SERVER_HOST = System.getenv("KUBERNETES_SERVICE_HOST");
    private static final String DEFAULT_API_SERVER_PORT = System.getenv("KUBERNETES_SERVICE_PORT");

    private String namespace = "default";
    private String apiServerHost = DEFAULT_API_SERVER_HOST;
    private int apiServerPort = StringUtils.isBlank(DEFAULT_API_SERVER_PORT) ? 443 : Integer.parseInt(DEFAULT_API_SERVER_PORT);
    private boolean verifySsl = true;
    private String authToken;

    public K8sConfig() {
        super(DataCenter.K8S);
    }

    public K8sConfig setNamespace(String ns) {
        Preconditions.checkNotNull(host, "k8s namespace cannot be null");
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

    public K8sConfig setAuthToken(String token)  {
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tokenFilePath)))) {
            return reader.readLine();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
