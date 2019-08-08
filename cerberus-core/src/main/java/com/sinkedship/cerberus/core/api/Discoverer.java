package com.sinkedship.cerberus.core.api;

import com.sinkedship.cerberus.commons.exception.CerberusException;
import com.sinkedship.cerberus.core.Service;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * A fundamental component of cerberus which defines the behaviours of:
 * finding all the services {@link Discoverer#findAll()},
 * finding all the service by name {@link Discoverer#findAllByName(String)}, etc.
 *
 * A service {@link Discoverer} only takes the responsibility of finding all the services
 * in data center. Providing a service to a customer is the job of service {@link Provider}.
 *
 * @author Derrick Guan
 */
public interface Discoverer {

    /**
     * Find all the services that are currently registered to the data center.
     *
     * @return a map of service name and corresponding service(s)' instance(s),
     *         an an empty non-null {@link Map} will be return if no service(s) has(have) been found.
     *
     * @throws CerberusException if not supported
     */
    @Nonnull
    default Map<String, List<Service>> findAll() throws CerberusException {
        throw new CerberusException("Not supported");
    }

    /**
     * Find all the services that are currently registered to the data center with specific service name.
     *
     * @param name service name
     *
     * @return service's instance(s), an empty {@link List} will be return
     *         if no service(s) has(have) been found.
     */
    @Nonnull
    List<Service> findAllByName(String name);

}


