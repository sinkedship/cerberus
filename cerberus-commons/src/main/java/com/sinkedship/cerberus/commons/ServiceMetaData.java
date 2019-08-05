package com.sinkedship.cerberus.commons;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Meta data about how to register and/or discovery a specific service.
 *
 * @author Derrick Guan
 */
public class ServiceMetaData {

    private final String organization;

    private final String category;

    private final String serviceName;

    public ServiceMetaData(String organization, String category, String serviceName) {
        Preconditions.checkArgument(!StringUtils.isBlank(organization), "service organization cannot be empty");
        Preconditions.checkArgument(!StringUtils.isBlank(category), "service category cannot be empty");
        Preconditions.checkArgument(!StringUtils.isBlank(serviceName), "service name cannot be empty");
        this.organization = organization;
        this.category = category;
        this.serviceName = serviceName;
    }

    public String getServiceIdentifier() {
        return String.format("%s-%s-%s", organization, category, serviceName);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceMetaData)) {
            return false;
        }
        ServiceMetaData that = (ServiceMetaData) obj;
        return this.organization.equalsIgnoreCase(that.organization) &&
                this.category.equalsIgnoreCase(that.category) &&
                this.serviceName.equalsIgnoreCase(that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organization, category, serviceName);
    }
}
