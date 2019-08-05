package com.sinkedship.cerberus.strategy;

import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Provider;

import java.util.List;
import java.util.Optional;

/**
 * A strategy that will not provide any service instances at all.
 *
 * @author Derrick Guan
 */
public class NullServiceStrategy implements Provider.Strategy {

    @Override
    public Optional<Service> choose(List<? extends Service> services) {
        // deliberately return null
        return Optional.empty();
    }

}
