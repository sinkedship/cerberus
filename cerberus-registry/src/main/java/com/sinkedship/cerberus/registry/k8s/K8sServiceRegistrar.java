package com.sinkedship.cerberus.registry.k8s;

import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Registrar;

/**
 * Blank implementation
 */
public class K8sServiceRegistrar implements Registrar {

    @Override
    public <S extends Service> boolean register(S service) {
        // Always return, because this registrar does not need to do anything about registering the service
        return true;
    }

    @Override
    public <S extends Service> boolean register(S service, long timeout) {
        // Always return, because this registrar does not need to do anything about registering the service
        return true;
    }

    @Override
    public <S extends Service> boolean unregister(S service) {
        // Always return, because this registrar does not need to do anything about registering the service
        return true;
    }
}
