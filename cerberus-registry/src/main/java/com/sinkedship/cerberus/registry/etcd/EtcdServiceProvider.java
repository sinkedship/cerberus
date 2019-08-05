package com.sinkedship.cerberus.registry.etcd;

import com.sinkedship.cerberus.commons.ServiceMetaData;
import com.sinkedship.cerberus.commons.config.data_center.EtcdConfig;
import com.sinkedship.cerberus.commons.utils.CerberusStringUtils;
import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Discoverer;
import com.sinkedship.cerberus.core.api.Provider;
import com.sinkedship.cerberus.strategy.RandomStrategy;
import com.sinkedship.cerberus.strategy.RoundRobinStrategy;
import io.etcd.jetcd.Client;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Derrick Guan
 */
public class EtcdServiceProvider implements Provider {

    private static final Logger LOGGER = LogManager.getLogger(EtcdServiceProvider.class);

    private final Discoverer discoverer;

    private final Provider.Strategy strategy = new RoundRobinStrategy();

    public EtcdServiceProvider(EtcdConfig config, Client etcdClient) {
        discoverer = new EtcdServiceDiscoverer(config, etcdClient);
    }

    @Override
    public Optional<Service> get(ServiceMetaData metaData) {
        return get(metaData.getServiceIdentifier());
    }

    @Override
    public Optional<Service> get(Class<?> targetClass) {
        return get(targetClass, CerberusStringUtils.DEFAULT_ASYNC_SUFFIX_SUPPLIER);
    }

    @Override
    public Optional<Service> get(Class<?> targetClass, Supplier<String> asyncSuffixSupplier) {
        return get(CerberusStringUtils.stripAsyncSuffix(targetClass.getCanonicalName(),
                asyncSuffixSupplier));
    }

    private Optional<Service> get(String serviceName) {
        List<Service> candidates = discoverer.findAllByName(serviceName);
        Optional<Service> ret = strategy.choose(candidates);
        if (!ret.isPresent()) {
            LOGGER.warn("Cannot resolve service:{} from Etcd", serviceName);
        }
        return ret;
    }
}
