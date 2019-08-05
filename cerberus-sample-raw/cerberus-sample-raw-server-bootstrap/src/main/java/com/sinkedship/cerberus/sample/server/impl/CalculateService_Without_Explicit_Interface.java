package com.sinkedship.cerberus.sample.server.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.airlift.drift.annotations.ThriftMethod;
import io.airlift.drift.annotations.ThriftService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;

/**
 * A service demonstrates that without explicitly implementing a thrift service
 * while providing the same functionality.
 *
 * @author Derrick Guan
 */
@ThriftService
public final class CalculateService_Without_Explicit_Interface {

    private static final Logger LOGGER = LogManager.getLogger(
            CalculateService_Without_Explicit_Interface.class);

    private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(
            Executors.newFixedThreadPool(10));

    @ThriftMethod
    public ListenableFuture<Integer> add(int a, int b) {
        LOGGER.info("RPC call add a: {} and b: {}", a, b);
        return executorService.submit(() -> a + b);
    }

    @ThriftMethod
    public ListenableFuture<Integer> verySlowGcd(int a, int b) {
        LOGGER.info("RPC call gcd a: {} and b: {}", a, b);
        return executorService.submit(() -> _gcd(a, b));
    }

    private int _gcd(int a, int b) {
        try {
            Thread.sleep(2 * 1000);
        } catch (Exception ignore) {
        }
        if (b == 0) {
            return a;
        } else {
            return _gcd(b, a % b);
        }
    }
}
