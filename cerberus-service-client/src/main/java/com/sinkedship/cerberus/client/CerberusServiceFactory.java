package com.sinkedship.cerberus.client;

import com.sinkedship.cerberus.client.config.CerberusClientConfig;
import com.sinkedship.cerberus.client.mimic.drift.CerberusDriftInvocationHandler;
import com.sinkedship.cerberus.client.mimic.drift.CerberusDriftMethodHandler;
import com.sinkedship.cerberus.commons.ServiceMetaData;
import com.sinkedship.cerberus.commons.utils.CerberusStringUtils;
import com.sinkedship.cerberus.core.api.Registry;
import com.sinkedship.cerberus.registry.DefaultRegistryFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.airlift.drift.client.DriftClient;
import io.airlift.drift.client.ExceptionClassifier;
import io.airlift.drift.client.RetryPolicy;
import io.airlift.drift.client.address.AddressSelector;
import io.airlift.drift.client.stats.MethodInvocationStat;
import io.airlift.drift.client.stats.MethodInvocationStatsFactory;
import io.airlift.drift.client.stats.NullMethodInvocationStat;
import io.airlift.drift.client.stats.NullMethodInvocationStatsFactory;
import io.airlift.drift.codec.ThriftCodecManager;
import io.airlift.drift.codec.metadata.ThriftMethodMetadata;
import io.airlift.drift.codec.metadata.ThriftServiceMetadata;
import io.airlift.drift.transport.MethodMetadata;
import io.airlift.drift.transport.client.Address;
import io.airlift.drift.transport.client.DriftClientConfig;
import io.airlift.drift.transport.client.MethodInvoker;
import io.airlift.drift.transport.client.MethodInvokerFactory;
import io.airlift.drift.transport.netty.client.DriftNettyMethodInvokerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import static com.google.common.reflect.Reflection.newProxy;
import static io.airlift.drift.transport.MethodMetadata.toMethodMetadata;

/**
 * Factory class that used to create a dynamic proxy of any Thrift service.
 *
 * @author Derrick Guan
 */
public final class CerberusServiceFactory {

    private final ThriftCodecManager codecManager;

    private final Supplier<MethodInvoker> methodInvokerSupplier;

    // cache a class with a specific address selector
    private final ConcurrentMap<Class<?>, AddressSelector<? extends Address>> addressSelectorCache;

    // cache a class with a corresponding thrift service meta data
    private final ConcurrentMap<Class<?>, ThriftServiceMetadata> serviceMetadataCache;

    private final MethodInvocationStatsFactory methodInvocationStatsFactory;

    private final Registry registry;

    public CerberusServiceFactory(CerberusClientConfig config) {
        this(config, DriftNettyMethodInvokerFactory.createStaticDriftNettyMethodInvokerFactory(
                config.getDriftNettyClientConfig()),
                new NullMethodInvocationStatsFactory());
    }

    public CerberusServiceFactory(CerberusClientConfig config,
                                  MethodInvocationStatsFactory methodInvocationStatsFactory) {
        this(config, DriftNettyMethodInvokerFactory.createStaticDriftNettyMethodInvokerFactory(
                config.getDriftNettyClientConfig()),
                methodInvocationStatsFactory);
    }

    public CerberusServiceFactory(CerberusClientConfig config, MethodInvokerFactory<?> methodInvokerFactory) {
        this(config, methodInvokerFactory, new NullMethodInvocationStatsFactory());
    }

    public CerberusServiceFactory(CerberusClientConfig config,
                                  MethodInvokerFactory<?> methodInvokerFactory,
                                  MethodInvocationStatsFactory methodInvocationStatsFactory) {
        Preconditions.checkNotNull(config, "Cerberus client config cannot be null");
        Preconditions.checkNotNull(methodInvokerFactory, "Method invoker factory cannot be null");
        Preconditions.checkNotNull(methodInvocationStatsFactory, "Method invocation stat factory cannot be null");
        codecManager = new ThriftCodecManager();
        methodInvokerSupplier = () -> methodInvokerFactory.createMethodInvoker(null);
        addressSelectorCache = new ConcurrentHashMap<>();
        serviceMetadataCache = new ConcurrentHashMap<>();
        this.methodInvocationStatsFactory = methodInvocationStatsFactory;

        registry = new DefaultRegistryFactory().createRegistry(config.getDataCenterConfig());
    }

    public <T> T newService(Class<T> clz) {
        return createDriftClient(clz,
                CerberusStringUtils.DEFAULT_ASYNC_SUFFIX_SUPPLIER,
                null, new DriftClientConfig())
                .get();
    }

    public <T> T newService(Class<T> clz, Supplier<String> asyncSuffixSupplier) {
        return createDriftClient(clz,
                null,
                null, new DriftClientConfig())
                .get();
    }

    public <T> T newService(Class<T> clz, Supplier<String> asyncSuffixSupplier, DriftClientConfig driftClientConfig) {
        return createDriftClient(clz,
                asyncSuffixSupplier,
                null,
                driftClientConfig)
                .get();
    }

    public <T> T newService(Class<T> clz, ServiceMetaData metaData) {
        return createDriftClient(clz,
                CerberusStringUtils.DEFAULT_ASYNC_SUFFIX_SUPPLIER,
                metaData,
                new DriftClientConfig())
                .get();
    }

    public <T> T newService(Class<T> clz, ServiceMetaData metaData, DriftClientConfig driftClientConfig) {
        return createDriftClient(clz,
                CerberusStringUtils.DEFAULT_ASYNC_SUFFIX_SUPPLIER,
                metaData,
                driftClientConfig)
                .get();
    }

    private <T> DriftClient<T> createDriftClient(Class<T> clientInterface,
                                                 Supplier<String> asyncSuffixSupplier,
                                                 ServiceMetaData metaData,
                                                 DriftClientConfig driftClientConfig) {
        AddressSelector<? extends Address> addressSelector = addressSelectorCache.computeIfAbsent(
                clientInterface,
                clz -> new CerberusAddressSelector(clientInterface, asyncSuffixSupplier, metaData, registry));

        ThriftServiceMetadata serviceMetadata = serviceMetadataCache.computeIfAbsent(
                clientInterface,
                clz -> new ThriftServiceMetadata(clz, codecManager.getCatalog())
        );
        MethodInvoker invoker = methodInvokerSupplier.get();
        ImmutableMap.Builder<Method, CerberusDriftMethodHandler> builder = ImmutableMap.builder();
        for (ThriftMethodMetadata method : serviceMetadata.getMethods().values()) {
            MethodMetadata metadata = toMethodMetadata(codecManager, method);

            RetryPolicy retryPolicy = new RetryPolicy(driftClientConfig, ExceptionClassifier.NORMAL_RESULT);

            MethodInvocationStat statHandler;
            if (driftClientConfig.isStatsEnabled()) {
                statHandler = methodInvocationStatsFactory.getStat(serviceMetadata, Optional.empty(), metadata);
            } else {
                statHandler = new NullMethodInvocationStat();
            }
            CerberusDriftMethodHandler handler = new CerberusDriftMethodHandler(
                    metadata, method.getHeaderParameters(),
                    invoker, method.isAsync(),
                    addressSelector, retryPolicy, statHandler);
            builder.put(method.getMethod(), handler);
        }
        Map<Method, CerberusDriftMethodHandler> methods = builder.build();
        return (context, headers) -> newProxy(clientInterface,
                new CerberusDriftInvocationHandler(serviceMetadata.getName(), methods, context, headers));
    }
}
