package com.sinkedship.cerberus.commons.config.data_center;

import com.sinkedship.cerberus.commons.DataCenter;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Derrick Guan
 */
public class EtcdConfig extends DataCenterConfig {

    private static final Logger LOGGER = LogManager.getLogger(EtcdConfig.class);

    private static final Endpoint.Scheme DEFAULT_SCHEME = Endpoint.Scheme.HTTP;
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 2379;

    private static final String ENDPOINT_FORMAT = "%s://%s:%d";
    private static final String DEFAULT_ENDPOINT = String.format(ENDPOINT_FORMAT,
            DEFAULT_SCHEME.getLiteral(), DEFAULT_HOST, DEFAULT_PORT);

    private static final String DEFAULT_KEY_PREFIX = "cerberus/services";
    private static final long DEFAULT_SERVICE_TTL = 5_000;
    private static final long DEFAULT_SERVICE_KEEP_INTERVAL = 3_000;

    private List<Endpoint> endpoints = new ArrayList<>();
    private String keyPrefix = DEFAULT_KEY_PREFIX;
    private long ttl = DEFAULT_SERVICE_TTL;
    private long keepInterval = DEFAULT_SERVICE_KEEP_INTERVAL;

    public EtcdConfig() {
        super(DataCenter.ETCD);
    }

    public EtcdConfig addEndpoint(Endpoint endpoint) {
        Preconditions.checkNotNull(endpoint, "Endpoint cannot be null");
        endpoints.add(endpoint);
        return this;
    }

    public List<URI> getEndpoints() {
        if (endpoints.isEmpty()) {
            try {
                URI uri = new URI(DEFAULT_ENDPOINT);
                return Collections.singletonList(uri);
            } catch (URISyntaxException e) {
                LOGGER.error("Invalid default endpoint:{}", DEFAULT_ENDPOINT);
                return Collections.emptyList();
            }
        } else {
            List<URI> ret = new ArrayList<>();
            for (Endpoint endpoint : endpoints) {
                try {
                    ret.add(endpoint.toUri());
                } catch (URISyntaxException e) {
                    LOGGER.error("Skip using invalid endpoint:{}", endpoint);
                }
            }
            return ret;
        }
    }

    public EtcdConfig setKeyPrefix(String prefix) {
        Preconditions.checkArgument(StringUtils.isNotBlank(prefix), "Etcd key prefix cannot be empty");
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        this.keyPrefix = prefix;
        return this;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public EtcdConfig setServiceTTL(long ttl) {
        Preconditions.checkArgument(ttl / 1000 > 0, "Service TTL is must > 1's after measuring in second");
        this.ttl = ttl;
        return this;
    }

    public long getServiceTTL() {
        return ttl;
    }

    public EtcdConfig setServiceKeepInterval(long interval) {
        Preconditions.checkArgument(ttl > 0, "Service keep interval must > 0");
        Preconditions.checkArgument(interval < ttl,
                "Service keep interval cannot longer than service TTL");
        this.keepInterval = interval;
        return this;
    }

    public long getServiceKeepInterval() {
        return keepInterval;
    }

    public final static class Endpoint {
        public enum Scheme {
            HTTP("http"),
            HTTPS("https");

            String literal;

            Scheme(String literal) {
                this.literal = literal;
            }

            public String getLiteral() {
                return this.literal;
            }

            public static Scheme getSchemeByName(String literal) {
                if (literal.equalsIgnoreCase(HTTP.literal)) {
                    return HTTP;
                } else if (literal.equalsIgnoreCase(HTTPS.literal)) {
                    return HTTPS;
                } else {
                    throw new IllegalArgumentException("Illegal scheme literal" + literal);
                }
            }
        }

        Scheme scheme;
        String host;
        int port;

        public Endpoint(Scheme scheme, String host, int port) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
        }

        public Endpoint(String host, int port) {
            this(DEFAULT_SCHEME, host, port);
        }

        public Endpoint(String host) {
            this(DEFAULT_SCHEME, host, DEFAULT_PORT);
        }

        public Endpoint() {
            this(DEFAULT_SCHEME, DEFAULT_HOST, DEFAULT_PORT);
        }

        URI toUri() throws URISyntaxException {
            return new URI(String.format(ENDPOINT_FORMAT, this.scheme.getLiteral(), host, port));
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.scheme, this.host, this.port);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Endpoint)) {
                return false;
            }
            Endpoint that = (Endpoint) obj;
            return this.scheme == that.scheme &&
                    this.host.equalsIgnoreCase(that.host) &&
                    this.port == that.port;
        }

        @Override
        public String toString() {
            return String.format(ENDPOINT_FORMAT, this.scheme.getLiteral(), this.host, this.port);
        }
    }
}
