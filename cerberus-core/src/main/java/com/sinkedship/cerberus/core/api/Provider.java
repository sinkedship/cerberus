package com.sinkedship.cerberus.core.api;

import com.sinkedship.cerberus.commons.ServiceMetaData;
import com.sinkedship.cerberus.core.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A fundamental component of cerberus which dedicates to take the responsibility of
 * providing a service instance to customers, with a certain {@link Strategy}
 * and a service discoverer {@link Discoverer} to fetch a fresh set of service instance(s).
 *
 * @author Derrick Guan
 */
public interface Provider {

    /**
     * Get a service instance with service meta data and default provider strategy.
     * Default provider strategy is specified in any concrete implementations.
     *
     * @param metaData of service
     *
     * @return a service instance, or {@link Optional} if no service instance is available.
     */
    Optional<Service> get(ServiceMetaData metaData);

    /**
     * Get a service instance with target class and default provider strategy.
     * Default provider strategy is specified in any concrete implementations.
     *
     * @param targetClass representing class of the service
     *
     * @return a service instance, or {@link Optional} if no service instance is available.
     */
    Optional<Service> get(Class<?> targetClass);

    /**
     * Get a service instance with target class, async suffix supplier and default provider strategy.
     * Default provider strategy is specified in any concrete implementations.
     *
     * @param targetClass         representing class of the service
     * @param asyncSuffixSupplier async suffix supplier
     *
     * @return a service instance, or {@link Optional} if no service instance is available.
     */
    Optional<Service> get(Class<?> targetClass, Supplier<String> asyncSuffixSupplier);

// TEMPORARY HIDDEN APIS

//    /**
//     * Get a service instance with name and provider strategy.
//     *
//     * @param name     service name
//     * @param strategy the way that used to choose a service from a bunch of instance(s).
//     *
//     * @return a service instance, or {@link Optional} if no service instance is available.
//     */
//    Optional<Service> get(String name, Strategy<X> strategy);
//
//    /**
//     * Get a service instance with target class and provider strategy.
//     *
//     * @param targetClass representing class of the service
//     * @param strategy    the way that used to choose a service from a bunch of instance(s).
//     *
//     * @return a service instance, or {@link Optional} if no service instance is available.
//     */
//    Optional<Service> get(Class<?> targetClass, Strategy strategy);

// TEMPORARY HIDDEN APIS

    interface Strategy {

        /**
         * Choose one service instance out of the instance(s) set.
         *
         * @param services a set of discovered service instance(s).
         *
         * @return a service instance, or {@link Optional} if the collection is empty.
         *
         * @see com.sinkedship.cerberus.strategy.RoundRobinStrategy
         * @see com.sinkedship.cerberus.strategy.RandomStrategy
         * @see com.sinkedship.cerberus.strategy.NullServiceStrategy
         */
        Optional<Service> choose(List<? extends Service> services);
    }

}
