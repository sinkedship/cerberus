package com.sinkedship.cerberus.client;

import com.sinkedship.cerberus.commons.ServiceMetaData;
import com.sinkedship.cerberus.commons.utils.CerberusStringUtils;
import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Provider;
import com.sinkedship.cerberus.core.api.Registry;
import com.google.common.net.HostAndPort;
import io.airlift.drift.client.address.AddressSelector;
import io.airlift.drift.client.address.SimpleAddressSelector;

import java.util.Optional;
import java.util.function.Supplier;


/**
 * Each instance of this class is dedicated to provide a valid address(if possible)
 * of Thrift service according to a specific class target.
 *
 * @author Derrick Guan
 */
public class CerberusAddressSelector implements AddressSelector<SimpleAddressSelector.SimpleAddress> {

    private final Class<?> targetClass;

    private final ServiceMetaData metaData;

    private final Supplier<String> asyncSuffixSupplier;

    private final Provider provider;

    public CerberusAddressSelector(Class<?> targetClass, Registry registry) {
        this(targetClass, CerberusStringUtils.DEFAULT_ASYNC_SUFFIX_SUPPLIER, null, registry);
    }

    public CerberusAddressSelector(Class<?> targetClass,
                                   Supplier<String> supplier,
                                   Registry registry) {
        this(targetClass, supplier, null, registry);
    }

    public CerberusAddressSelector(Class<?> targetClass,
                                   ServiceMetaData metaData,
                                   Registry registry) {
        this(targetClass, CerberusStringUtils.DEFAULT_ASYNC_SUFFIX_SUPPLIER, metaData, registry);
    }

    public CerberusAddressSelector(Class<?> targetClass,
                                   Supplier<String> supplier,
                                   ServiceMetaData metaData,
                                   Registry registry) {
        this.targetClass = targetClass;
        this.metaData = metaData;
        this.asyncSuffixSupplier = supplier;
        this.provider = registry.provider();
    }

    @Override
    public Optional<SimpleAddressSelector.SimpleAddress> selectAddress(Optional<String> addressSelectionContext) {
        // if meta data presents, use it as the first priority and then falls back to the other
        Optional<Service> service;
        if (metaData != null) {
            service = provider.get(metaData);
        } else {
            service = provider.get(targetClass, asyncSuffixSupplier);
        }

        return service.flatMap(cerberusService -> Optional.of(new SimpleAddressSelector.SimpleAddress(
                HostAndPort.fromParts(cerberusService.getHost(), cerberusService.getPort()))));
    }
}
