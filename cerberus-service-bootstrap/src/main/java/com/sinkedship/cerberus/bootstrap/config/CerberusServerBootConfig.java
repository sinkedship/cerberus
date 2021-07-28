package com.sinkedship.cerberus.bootstrap.config;

import com.google.common.base.Preconditions;
import io.airlift.drift.transport.netty.server.DriftNettyServerConfig;
import com.sinkedship.cerberus.commons.utils.HostAndPortUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Derrick Guan
 */
public class CerberusServerBootConfig {

    private String host = HostAndPortUtils.getDefaultHost();

    private DriftNettyServerConfig driftServerConfig;

    private int logicThreadCount = 200;

    public CerberusServerBootConfig() {
        driftServerConfig = new DriftNettyServerConfig();
        this.setPort(HostAndPortUtils.getAvailablePort());
    }

    public String getHost() {
        return host;
    }

    public CerberusServerBootConfig setHost(String host) {
        Preconditions.checkArgument(!StringUtils.isBlank(host), "binding host cannot be empty");
        this.host = host;
        return this;
    }

    public int getPort() {
        return driftServerConfig.getPort();
    }

    public CerberusServerBootConfig setPort(int port) {
        Preconditions.checkArgument(port >= HostAndPortUtils.PORT_RANGE_MIN &&
                        port <= HostAndPortUtils.PORT_RANGE_MAX,
                "binding port out of range, should be in [1024, 65535]");
        driftServerConfig = driftServerConfig.setPort(port);
        return this;
    }

    public int getAcceptBacklog() {
        return driftServerConfig.getAcceptBacklog();
    }

    public CerberusServerBootConfig setAcceptBacklog(int acceptBacklog) {
        driftServerConfig = driftServerConfig.setAcceptBacklog(acceptBacklog);
        return this;
    }

    public int getIoThreadCount() {
        return driftServerConfig.getIoThreadCount();
    }

    public CerberusServerBootConfig setIoThreadCount(int ioThreadCount) {
        driftServerConfig = driftServerConfig.setIoThreadCount(ioThreadCount);
        return this;
    }

    public int getWorkerThreadCount() {
        return driftServerConfig.getWorkerThreadCount();
    }

    public CerberusServerBootConfig setWorkerThreadCount(int workerThreadCount) {
        driftServerConfig = driftServerConfig.setWorkerThreadCount(workerThreadCount);
        return this;
    }

    public Duration getRequestTimeout() {
        return Duration.ofMillis(driftServerConfig.getRequestTimeout().toMillis());
    }

    public CerberusServerBootConfig setRequestTimeout(Duration requestTimeout) {
        driftServerConfig = driftServerConfig.setRequestTimeout(
                io.airlift.units.Duration.succinctDuration(requestTimeout.toMillis(),
                        TimeUnit.MILLISECONDS));
        return this;
    }

    public boolean allowPlaintext() {
        return driftServerConfig.isAllowPlaintext();
    }

    public CerberusServerBootConfig setAllowPlaintext(boolean allowPlaintext) {
        driftServerConfig = driftServerConfig.setAllowPlaintext(allowPlaintext);
        return this;
    }

    public Duration getSslContextRefreshTime() {
        return Duration.ofMillis(driftServerConfig.getSslContextRefreshTime().toMillis());
    }

    public CerberusServerBootConfig setSslContextRefreshTime(Duration sslContextRefreshTime) {
        driftServerConfig = driftServerConfig.setSslContextRefreshTime(
                io.airlift.units.Duration.succinctDuration(sslContextRefreshTime.toMillis(),
                        TimeUnit.MILLISECONDS));
        return this;
    }

    public boolean sslEnable() {
        return driftServerConfig.isSslEnabled();
    }

    public CerberusServerBootConfig enableSsl(boolean enableSsl) {
        driftServerConfig = driftServerConfig.setSslEnabled(enableSsl);
        return this;
    }

    public File getSslTrustCertificate() {
        return driftServerConfig.getTrustCertificate();
    }

    public CerberusServerBootConfig setSslTrustCertificate(File trustCertificate) {
        driftServerConfig = driftServerConfig.setTrustCertificate(trustCertificate);
        return this;
    }

    public File getSslKey() {
        return driftServerConfig.getKey();
    }

    public CerberusServerBootConfig setSslKey(File key) {
        driftServerConfig = driftServerConfig.setKey(key);
        return this;
    }

    public String getSslKeyPassword() {
        return driftServerConfig.getKeyPassword();
    }

    public CerberusServerBootConfig setSslKeyPassword(String password) {
        driftServerConfig = driftServerConfig.setKeyPassword(password);
        return this;
    }

    public long getSslSessionCacheSize() {
        return driftServerConfig.getSessionCacheSize();
    }

    public CerberusServerBootConfig setSslSessionCacheSize(long sessionCacheSize) {
        driftServerConfig = driftServerConfig.setSessionCacheSize(sessionCacheSize);
        return this;
    }

    public Duration getSslSessionTimeout() {
        return Duration.ofMillis(driftServerConfig.getSessionTimeout().toMillis());
    }

    public CerberusServerBootConfig setSslSessionTimeout(Duration sessionTimeout) {
        driftServerConfig = driftServerConfig.setSessionTimeout(
                io.airlift.units.Duration.succinctDuration(sessionTimeout.toMillis(),
                        TimeUnit.MILLISECONDS));
        return this;
    }

    public List<String> getSslCiphers() {
        return driftServerConfig.getCiphers();
    }

    public CerberusServerBootConfig setSslCiphers(String ciphers) {
        driftServerConfig = driftServerConfig.setCiphers(ciphers);
        return this;
    }

    public DriftNettyServerConfig getUnderlyingConfig() {
        return driftServerConfig;
    }

    public int getLogicThreadCount() {
        return logicThreadCount;
    }

    public void setLogicThreadCount(int logicThreadCount) {
        this.logicThreadCount = logicThreadCount;
    }
}
