package com.sinkedship.cerberus.registry.consul;

import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Registrar;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

/**
 * @author Derrick Guan
 */
public class ConsulServiceRegistrar implements Registrar {

    private static final Logger LOGGER = LogManager.getLogger(ConsulServiceRegistrar.class);

    private final ConsulClient consulClient;

    private final ConsulServiceKeeper keeper;

    public ConsulServiceRegistrar(ConsulClient consulClient) {
        Preconditions.checkNotNull(consulClient, "Consul client cannot be null");
        this.consulClient = consulClient;
        keeper = new ConsulServiceKeeper(consulClient);
        new Thread(keeper).start();
    }

    @Override
    public <S extends Service> boolean register(S service) {
        if (StringUtils.isBlank(service.getId())) {
            LOGGER.error("Aborting registration, service:{} with an empty " +
                    "identifier cannot be registered to Consul", service.getIdentifier());
            return false;
        }
        NewService consulService = buildConsulService(service);
        try {
            // HTTP succeeds or throws exception from Consul client
            consulClient.agentServiceRegister(consulService);
            LOGGER.debug("Register service:{}, id:{} to Consul successfully",
                    service.getIdentifier(), service.getId());
            keeper.addServiceId(service.getId());
            return true;
        } catch (Throwable t) {
            LOGGER.error("Unable to register service:{}, id:{} to Consul due:",
                    service.getIdentifier(), service.getId(), t);
            return false;
        }
    }

    @Override
    public <S extends Service> boolean register(S service, long timeout) {
        throw new UnsupportedOperationException(
                "ConsulServiceRegistrar does not support registering service with timeout by now");
    }

    @Override
    public <S extends Service> boolean unregister(S service) {
        if (StringUtils.isBlank(service.getId())) {
            LOGGER.error("Cannot unregister a service:{} with an empty identifier",
                    service.getIdentifier());
            return false;
        }

        try {
            consulClient.agentServiceDeregister(service.getId());
            LOGGER.debug("Unregister service:{}, id:{} from Consul successfully",
                    service.getIdentifier(), service.getId());
            keeper.removeServiceId(service.getId());
            return true;
        } catch (Throwable t) {
            LOGGER.error("Unable to unregister service:{}, id:{} from Consul due to:",
                    service.getIdentifier(), service.getId(), t);
            return false;
        }
    }

    private NewService buildConsulService(Service s) {
        NewService consulService = new NewService();
        consulService.setId(s.getId());
        consulService.setName(s.getIdentifier());
        consulService.setAddress(s.getHost());
        consulService.setPort(s.getPort());
        consulService.setTags(Collections.singletonList(s.getName()));
        NewService.Check check = new NewService.Check();
        check.setDeregisterCriticalServiceAfter("5s");
        check.setTtl("5s");
        consulService.setCheck(check);
        return consulService;
    }

}


