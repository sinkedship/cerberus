package com.sinkedship.cerberus.sample.api.service;

import com.google.common.util.concurrent.ListenableFuture;
import io.airlift.drift.annotations.ThriftMethod;
import io.airlift.drift.annotations.ThriftService;

/**
 * @author Derrick Guan
 */
@ThriftService
public interface HelloService {

    @ThriftMethod
    String hello(String from, boolean mockTimeConsumingOperation);

    @ThriftMethod(value = "another_hello")
    String hello(int number, boolean mockTimeConsumingOperation);


    @ThriftService
    interface Async {
        @ThriftMethod
        ListenableFuture<String> hello(String from, boolean mockTimeConsumingOperation);

        @ThriftMethod(value = "another_hello")
        ListenableFuture<String> hello(int number, boolean mockTimeConsumingOperation);
    }

}
