package com.sinkedship.cerberus.commons;

import java.util.Objects;

public class K8sServiceMetaData extends ServiceMetaData {

    private final String serviceName;

    private final String servicePortName;

    public K8sServiceMetaData(String serviceName, String servicePortName) {
        super("", "", "");
        this.serviceName = serviceName;
        this.servicePortName = servicePortName;
    }

    @Override
    public String getServiceIdentifier() {
        return serviceName;
    }

    public String getServicePortName() {
        return servicePortName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof K8sServiceMetaData)) {
            return false;
        }
        K8sServiceMetaData that = (K8sServiceMetaData) obj;
        return that.serviceName.equalsIgnoreCase(this.serviceName) &&
                that.servicePortName.equalsIgnoreCase(this.servicePortName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.serviceName, this.servicePortName);
    }
}
