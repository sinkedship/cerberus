package com.sinkedship.cerberus.registry.local;

import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Registrar;

/**
 * Local registrar will not actually register any services.
 *
 * @author Derrick Guan
 */
public class LocalServiceRegistrar implements Registrar {

    @Override
    public <S extends Service> boolean register(S service) {
        return true;
    }

    @Override
    public <S extends Service> boolean register(S service, long timeout) {
        return true;
    }

    @Override
    public <S extends Service> boolean unregister(S service) {
        return true;
    }
}
