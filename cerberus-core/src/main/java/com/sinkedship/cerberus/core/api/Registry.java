package com.sinkedship.cerberus.core.api;

import java.io.Closeable;

/**
 * A central place where provides a service provider {@link Provider}
 * and a service registrar {@link Registrar} for service consumer who wants to get and use a service
 * while for service supplier (which will be called provider in some others situations,
 * but in cerberus we will keep it called supplier to be distinct from the term {@link Provider}),
 * who can register itself to data center respectively.
 *
 * @author Derrick Guan
 */
public interface Registry extends Closeable {

    /**
     * Get a service provider from this registry.
     *
     * @return provider
     */
    Provider provider();

    /**
     * Get a service registrar from this registry.
     *
     * @return registrar
     */
    Registrar registrar();

}
