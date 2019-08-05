package com.sinkedship.cerberus.sample.api.service;

import com.google.common.util.concurrent.ListenableFuture;
import io.airlift.drift.annotations.ThriftMethod;
import io.airlift.drift.annotations.ThriftService;

/**
 * @author Derrick Guan
 */
@ThriftService
public interface CalculateService {

    @ThriftMethod
    int add(int a, int b);

    @ThriftMethod
    int verySlowGcd(int a, int b);

    @ThriftService
    interface Async {

        @ThriftMethod
        ListenableFuture<Integer> add(int a, int b);

        @ThriftMethod
        ListenableFuture<Integer> verySlowGcd(int a, int b);
    }

}
