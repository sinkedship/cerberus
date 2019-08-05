package com.sinkedship.cerberus.sample.server.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.sinkedship.cerberus.sample.api.service.HelloService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;
import java.util.concurrent.Executors;

/**
 * @author Derrick Guan
 */
public class HelloServiceImpl implements HelloService.Async {

    private static final Logger LOGGER = LogManager.getLogger(HelloServiceImpl.class);

    private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(
            Executors.newFixedThreadPool(10));

    private final Random random = new Random();

    @Override
    public ListenableFuture<String> hello(String from, boolean mockTimeConsumingOperation) {
        LOGGER.info("request from: {}", from);
        return executorService.submit(() -> {
            try {
                if (mockTimeConsumingOperation) {
                    Thread.sleep(random.nextInt(10) * 1000);
                }
            } catch (Exception ignore) {

            }
            return String.format("Hello %s, this is RPC server responding!", from);
        });
    }

    @Override
    public ListenableFuture<String> hello(int number, boolean mockTimeConsumingOperation) {
        LOGGER.info("request from: {}", number);
        return executorService.submit(() -> {
            try {
                if (mockTimeConsumingOperation) {
                    Thread.sleep(random.nextInt(10) * 1000);
                }
            } catch (Exception ignore) {

            }
            return String.format("Hello number %d, this is RPC server responding!", number);
        });
    }
}
