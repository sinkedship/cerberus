package com.sinkedship.cerberus.bootstrap;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.sinkedship.cerberus.bootstrap.config.CerberusServerBootConfig;
import com.sinkedship.cerberus.bootstrap.config.CerberusServerConfig;
import com.sinkedship.cerberus.bootstrap.netty.transport.CerberusNettyServerTransportFactory;
import com.sinkedship.cerberus.commons.DataCenter;
import com.sinkedship.cerberus.commons.ServiceMetaData;
import com.sinkedship.cerberus.commons.exception.CerberusException;
import com.sinkedship.cerberus.commons.utils.ReflectionUtils;
import com.sinkedship.cerberus.core.CerberusService;
import com.sinkedship.cerberus.core.api.Registrar;
import com.sinkedship.cerberus.core.api.Registry;
import com.sinkedship.cerberus.core.api.RegistryFactory;
import com.sinkedship.cerberus.registry.DefaultRegistryFactory;
import io.airlift.drift.annotations.ThriftService;
import io.airlift.drift.codec.ThriftCodecManager;
import io.airlift.drift.server.DriftServer;
import io.airlift.drift.server.DriftService;
import io.airlift.drift.server.stats.JmxMethodInvocationStatsFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.weakref.jmx.MBeanExporter;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A bootstrap for thrift services to run and register themselves to a specific data center.
 *
 * @author Derrick Guan
 */
public class CerberusServerBootstrap {

    private static final Logger LOGGER = LogManager.getLogger(CerberusServerBootstrap.class);

    private final Set<DriftServiceWrapper> wrappers;

    private final CerberusServerConfig config;

    private final Registry registry;

    private final RegisterSuccessHandler<CerberusService> successHandler;

    private final RegisterFailureHandler<CerberusService> failureHandler;

    public CerberusServerBootstrap(CerberusServerConfig config,
                                   Set<DriftServiceWrapper> services,
                                   Registry registry) {
        this(config, services, registry, null, null);
    }

    public CerberusServerBootstrap(CerberusServerConfig config,
                                   Set<DriftServiceWrapper> wrappers,
                                   Registry registry,
                                   RegisterSuccessHandler<CerberusService> successHandler,
                                   RegisterFailureHandler<CerberusService> failureHandler) {
        Preconditions.checkNotNull(config, "Cerberus server config is null");
        Preconditions.checkNotNull(registry, "Registry is null");
        Preconditions.checkNotNull(wrappers, "Services set cannot be null");

        if (wrappers.size() <= 0) {
            LOGGER.warn("Cerberus is not going to serve any services");
            throw new RuntimeException("empty thrift services set");
        }
        this.config = config;
        this.wrappers = wrappers;
        this.registry = registry;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    public static class Builder {
        private final Map<Object, Pair<ServiceMetaData, Supplier<String>>> metaMap;

        private final DataCenter dataCenter;
        private RegistryFactory registryFactory;
        private CerberusServerConfig config;

        private RegisterSuccessHandler<CerberusService> successHandler;
        private RegisterFailureHandler<CerberusService> failureHandler;

        public Builder(DataCenter dataCenter) {
            this.dataCenter = dataCenter;
            metaMap = new HashMap<>();
        }

        public Builder(CerberusServerConfig config) {
            Preconditions.checkNotNull(config, "Cerberus server config cannot be null");
            this.config = config;
            this.dataCenter = config.getDataCenterConfig().getDataCenter();
            metaMap = new HashMap<>();
        }

        public Builder withServerConfig(CerberusServerConfig config) {
            Preconditions.checkNotNull(config, "Cerberus server config cannot be null");
            Preconditions.checkArgument(this.dataCenter == config.getDataCenterConfig().getDataCenter(),
                    "different data center between constructor and Cerberus server configuration");
            this.config = config;
            return this;
        }

        public Builder withService(Object service) {
            return withService(service, null, null);
        }

        public Builder withService(Object service, Supplier<String> asyncSuffixSupplier) {
            return withService(service, null, asyncSuffixSupplier);
        }

        public Builder withService(Object service, ServiceMetaData metaData) {
            return withService(service, metaData, null);
        }

        public Builder withService(Object service, ServiceMetaData metaData, Supplier<String> asyncSuffixSupplier) {
            Preconditions.checkNotNull(service, "service cannot be null");
            metaMap.put(service, Pair.of(metaData, asyncSuffixSupplier));
            return this;
        }

        public Builder withRegistryFactory(RegistryFactory registryFactory) {
            Preconditions.checkNotNull(config, "registry factory cannot be null");
            this.registryFactory = registryFactory;
            return this;
        }

        public Builder withRegisterSuccessHandler(RegisterSuccessHandler<CerberusService> successHandler) {
            if (successHandler == null) {
                LOGGER.warn("Setting null to RegisterSuccessHandler which doesn't make any sense");
            }
            this.successHandler = successHandler;
            return this;
        }

        public Builder withRegisterFailureHandler(RegisterFailureHandler<CerberusService> failureHandler) {
            if (failureHandler == null) {
                LOGGER.warn("Setting null to RegisterFailureHandler which doesn't make any sense");
            }
            this.failureHandler = failureHandler;
            return this;
        }

        public CerberusServerBootstrap build() {
            if (config == null) {
                config = new CerberusServerConfig(this.dataCenter);
            }
            // validate service meta data info, or throw exception
            if (!validateServiceMetadata(metaMap.values())) {
                throw new CerberusException("Duplicated service meta data for different services");
            }

            Set<DriftServiceWrapper> driftServices = new HashSet<>();
            for (Map.Entry<Object, Pair<ServiceMetaData, Supplier<String>>> entry : metaMap.entrySet()) {
                Object service = entry.getKey();
                Pair<ServiceMetaData, Supplier<String>> p = entry.getValue();
                DriftServiceWrapper driftService = new DriftServiceWrapper(
                        new DriftService(service, Optional.empty(), true),
                        p.getLeft(), p.getRight());
                driftServices.add(driftService);
            }

            if (registryFactory == null) {
                registryFactory = new DefaultRegistryFactory();
            }
            Registry registry = registryFactory.createRegistry(config.getDataCenterConfig());
            return new CerberusServerBootstrap(config, driftServices, registry, successHandler, failureHandler);
        }

        private boolean validateServiceMetadata(Collection<Pair<ServiceMetaData, Supplier<String>>> pairs) {
            List<ServiceMetaData> nonEmptyList = pairs.stream().filter(p -> p.getLeft() != null)
                    .map(Pair::getLeft).collect(Collectors.toList());
            Set<ServiceMetaData> nonEmptySet = new HashSet<>(nonEmptyList);
            return !(nonEmptySet.size() < nonEmptyList.size());
        }
    }

    public void boot() throws CerberusException {
        try {
            // try to run all the thrift service
            run(config.getBootConfig(), wrappers);
        } catch (RuntimeException e) {
            // throw by netty transport when it tries to start
            LOGGER.error("Error raises while trying to boot all the services", e);
            // throws it to caller
            throw new CerberusException(e);
        }
        registerService(config, wrappers);
    }

    // actual thrift services start-up
    private void run(CerberusServerBootConfig bootConfig, Set<DriftServiceWrapper> wrappers) {
        Set<DriftService> services = wrappers.stream().map(DriftServiceWrapper::getDriftService)
                .collect(Collectors.toSet());
        DriftServer server = new DriftServer(
                new CerberusNettyServerTransportFactory(bootConfig),
                new ThriftCodecManager(),
                new JmxMethodInvocationStatsFactory(new MBeanExporter(ManagementFactory.getPlatformMBeanServer())),
                ImmutableSet.copyOf(services),
                ImmutableSet.of()
        );
        server.start();
    }

    private void registerService(CerberusServerConfig config, Set<DriftServiceWrapper> wrappers) {
        Registrar registrar = registry.registrar();

        List<CerberusService> successList = new ArrayList<>();
        List<CerberusService> failureList = new ArrayList<>();

        for (DriftServiceWrapper wrapper : wrappers) {
            Object svr = wrapper.getDriftService().getService();
            Set<Class<?>> targetThriftClassSet = ReflectionUtils.getEffectiveClassByAnnotation(
                    svr.getClass(), ThriftService.class);
            if (targetThriftClassSet.isEmpty()) {
                LOGGER.error("Cannot find annotated @ThriftService from service:{}",
                        svr.getClass());
                throw new CerberusException("Cannot find thrift service from class" +
                        svr.getClass());
            }
            if (targetThriftClassSet.size() > 1) {
                LOGGER.error("More than one annotated @ThriftService from service:{}",
                        svr.getClass());
                throw new CerberusException("More than one thrift interface from class" +
                        svr.getClass());
            }
            Class<?> targetThriftClass = Iterables.getOnlyElement(targetThriftClassSet);
            CerberusService cerberusService = new CerberusService
                    .Builder(targetThriftClass)
                    .metaData(wrapper.getMetaData().orElse(null))
                    .suffixSupplier(wrapper.getAsyncSuffixSupplier().orElse(null))
                    .host(config.getDataCenterConfig().getRegisterHost().orElse(""))
                    .port(config.getBootConfig().getPort())
                    .build();
            if (registrar.register(cerberusService)) {
                successList.add(cerberusService);
            } else {
                failureList.add(cerberusService);
            }
        }
        if (successHandler != null) {
            successHandler.onSuccess(successList);
        }
        if (failureHandler != null) {
            failureHandler.onFailure(failureList);
        }
    }

}
