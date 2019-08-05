package com.sinkedship.cerberus.core.api;

import com.sinkedship.cerberus.commons.DataCenter;
import com.sinkedship.cerberus.core.Service;

/**
 * A fundamental component of Cerberus which defines the basic behaviours of,
 * registering service(s) {@link Registrar#register(Service)} to or
 * un-registering service(s) {@link Registrar#unregister(Service)} from a
 * data center {@link DataCenter}, respectively.
 *
 * @author Derrick Guan
 */
public interface Registrar {

    /**
     * Register a service to a particular data center.
     *
     * @param service which needs to be registered.
     *
     * @return {@code true} means registering the service to data center successfully, {@code false} otherwise.
     *
     * @see com.sinkedship.cerberus.core.api.Registrar#register(Service, long)
     */
    <S extends Service> boolean register(S service);

    /**
     * Register a service to a particular data center with a timeout.
     *
     * @param service which needs to be registered.
     * @param timeout the maximum time to try registering in milliseconds.
     *
     * @return {@code true} means registering the service to data center successfully;
     *         {@code false} means the failure of registering or it's completed in the dedicated {@code timeout}.
     *
     * @see com.sinkedship.cerberus.core.api.Registrar#register(Service)
     */
    <S extends Service> boolean register(S service, long timeout);

    /**
     * Un-register a service from a particular data center.
     *
     * @param service which needs to be un-registered.
     *
     * @return {@code true} means un-registering the service from data center successfully;
     *         {@code false} otherwise.
     */
    <S extends Service> boolean unregister(S service);

}
