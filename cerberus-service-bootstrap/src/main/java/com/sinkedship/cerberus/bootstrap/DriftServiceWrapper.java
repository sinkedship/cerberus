package com.sinkedship.cerberus.bootstrap;

import com.google.common.base.Preconditions;
import io.airlift.drift.server.DriftService;
import com.sinkedship.cerberus.commons.ServiceMetaData;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Derrick Guan
 */
final class DriftServiceWrapper {

    private final DriftService driftService;

    private final ServiceMetaData metaData;

    private final Supplier<String> asyncSuffixSupplier;

    DriftServiceWrapper(DriftService driftService, ServiceMetaData metaData, Supplier<String> asyncSuffixSupplier) {
        Preconditions.checkNotNull(driftService, "drift service cannot be null");
        this.driftService = driftService;
        this.metaData = metaData;
        this.asyncSuffixSupplier = asyncSuffixSupplier;
    }

    final DriftService getDriftService() {
        return driftService;
    }

    final Optional<Supplier<String>> getAsyncSuffixSupplier() {
        return Optional.ofNullable(asyncSuffixSupplier);
    }

    final Optional<ServiceMetaData> getMetaData() {
        return Optional.ofNullable(metaData);
    }
}
