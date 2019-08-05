package com.sinkedship.cerberus.core;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.Serializable;
import java.util.Objects;

/**
 * Base Service
 *
 * @author Derrick Guan
 */
public class Service implements Serializable {

    // Identifier of service
    protected final String identifier;

    // Readable name of this service
    protected final String name;

    // Identity of this service instance
    protected final String id;

    // Host of this service instance
    protected final String host;

    // Port of this service instance
    protected final int port;

    public Service(String identifier, String name, String id, String host, int port) {
        this.identifier = identifier;
        this.name = name;
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Service)) {
            return false;
        }

        Service that = (Service) obj;
        return this.identifier.equalsIgnoreCase(that.identifier) &&
                this.name.equalsIgnoreCase(that.name) &&
                this.id.equalsIgnoreCase(that.id) &&
                this.host.equalsIgnoreCase(that.host) &&
                this.port == that.port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.identifier, this.name, this.id, this.host, this.port);
    }
}
