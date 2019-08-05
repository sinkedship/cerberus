package com.sinkedship.cerberus.strategy;

import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Provider;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * A strategy that chooses a random service instance from available services collection.
 *
 * @author Derrick Guan
 */
public class RandomStrategy implements Provider.Strategy {

    private final Random random = new Random();

    @Override
    public Optional<Service> choose(List<? extends Service> services) {
        if (services.isEmpty()) {
            return Optional.empty();
        } else {
            Service service = services.get(random.nextInt(services.size()));
            return Optional.of(service);
        }
    }
}
