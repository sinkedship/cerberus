package com.sinkedship.cerberus.core;

import com.sinkedship.cerberus.commons.ServiceMetaData;
import com.sinkedship.cerberus.commons.utils.CerberusStringUtils;
import com.sinkedship.cerberus.commons.utils.HostAndPortUtils;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * An abstraction of a service.
 * <p>
 * Provides the basic information of it.
 * <p/>
 *
 * @author Derrick Guan
 */
public final class CerberusService extends AbstractService implements Serializable {

    private CerberusService(String identifier, String name, String id, String host, int port,
                            int version, long startUpTs, boolean enable) {
        super(identifier, name, id, host, port, version, startUpTs, enable);
    }

    public static class Builder {
        private Class<?> clz;
        private Supplier<String> suffixSupplier;
        private ServiceMetaData metaData;
        private String name;
        private int version;
        private boolean enable = true;
        private String id;
        private String host;
        private int port;

        public Builder(Class<?> clz) {
            Preconditions.checkNotNull(clz, "target class cannot be null");
            this.clz = clz;
        }

        public Builder metaData(ServiceMetaData metaData) {
            this.metaData = metaData;
            return this;
        }

        public Builder suffixSupplier(Supplier<String> supplier) {
            this.suffixSupplier = supplier;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder enable(boolean enable) {
            this.enable = enable;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public CerberusService build() {
            String identifier;
            if (metaData != null) {
                identifier = metaData.getServiceIdentifier();
            } else {
                if (suffixSupplier == null) {
                    identifier = CerberusStringUtils.stripAsyncSuffix(clz.getCanonicalName());
                } else {
                    identifier = CerberusStringUtils.stripAsyncSuffix(clz.getCanonicalName(), suffixSupplier);
                }
            }
            if (StringUtils.isBlank(name)) {
                this.name = identifier;
            }
            if (StringUtils.isBlank(id)) {
                this.id = UUID.randomUUID().toString();
            }
            if (StringUtils.isBlank(host)) {
                this.host = HostAndPortUtils.getDefaultHost();
            }
            if (port < HostAndPortUtils.PORT_RANGE_MIN || port > HostAndPortUtils.PORT_RANGE_MAX) {
                this.port = HostAndPortUtils.getAvailablePort();
            }

            return new CerberusService(identifier, name, id, host, port,
                    version, System.currentTimeMillis(), enable);
        }
    }

}
