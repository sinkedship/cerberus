package com.sinkedship.cerberus.strategy;

import com.sinkedship.cerberus.core.Service;
import com.sinkedship.cerberus.core.api.Provider;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A strategy that rotates and chooses a service instance through a sequential list.
 * <p>
 * However, it's not so strict round-robin fashion, I don't put the increment and decrement
 * of the services in consideration because of the simplicity of implementation
 *
 * @author Derrick Guan
 */
public class RoundRobinStrategy implements Provider.Strategy {

    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public Optional<Service> choose(List<? extends Service> services) {
        if (services.isEmpty()) {
            return Optional.empty();
        } else {
            int nowIndex = Math.abs(index.getAndIncrement());
            return Optional.of(services.get(nowIndex % services.size()));
        }
    }
}
