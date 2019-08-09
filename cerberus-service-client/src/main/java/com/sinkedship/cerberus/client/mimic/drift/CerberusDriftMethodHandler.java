package com.sinkedship.cerberus.client.mimic.drift;

import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import io.airlift.drift.client.RetryPolicy;
import io.airlift.drift.client.address.AddressSelector;
import io.airlift.drift.client.stats.MethodInvocationStat;
import io.airlift.drift.codec.metadata.ThriftHeaderParameter;
import io.airlift.drift.transport.MethodMetadata;
import io.airlift.drift.transport.client.Address;
import io.airlift.drift.transport.client.MethodInvoker;

import java.util.*;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

/**
 * A completely mimic of io.airlift.drift.client.DriftMethodHandler
 * excepts that it's public.
 *
 * @author Derrick Guan
 */
public class CerberusDriftMethodHandler {

    private final MethodMetadata metadata;
    private final Map<Integer, ThriftHeaderParameter> headerParameters;
    private final MethodInvoker invoker;
    private final boolean async;
    private final AddressSelector<? extends Address> addressSelector;
    private final RetryPolicy retryPolicy;
    private final MethodInvocationStat stat;

    public CerberusDriftMethodHandler(
            MethodMetadata metadata,
            Set<ThriftHeaderParameter> headersParameters,
            MethodInvoker invoker,
            boolean async,
            AddressSelector<? extends Address> addressSelector,
            RetryPolicy retryPolicy,
            MethodInvocationStat stat) {
        this.metadata = requireNonNull(metadata, "metadata is null");
        this.headerParameters = requireNonNull(headersParameters, "headersParameters is null").stream()
                .collect(toImmutableMap(ThriftHeaderParameter::getIndex, identity()));
        this.invoker = requireNonNull(invoker, "invoker is null");
        this.async = async;
        this.addressSelector = requireNonNull(addressSelector, "addressSelector is null");
        this.retryPolicy = retryPolicy;
        this.stat = requireNonNull(stat, "stat is null");
    }

    public boolean isAsync() {
        return async;
    }

    public ListenableFuture<Object> invoke(Optional<String> addressSelectionContext, Map<String, String> headers,
                                           List<Object> parameters) {
        if (!headerParameters.isEmpty()) {
            headers = new LinkedHashMap<>(headers);
            for (Map.Entry<Integer, ThriftHeaderParameter> entry : headerParameters.entrySet()) {
                String headerValue = (String) parameters.get(entry.getKey());
                if (headerValue != null) {
                    headers.put(entry.getValue().getName(), headerValue);
                }
            }

            ImmutableList.Builder<Object> newParameters = ImmutableList.builder();
            for (int index = 0; index < parameters.size(); index++) {
                if (!headerParameters.containsKey(index)) {
                    newParameters.add(parameters.get(index));
                }
            }
            parameters = newParameters.build();
        }
        return CerberusDriftMethodInvocation.createDriftMethodInvocation(
                invoker, metadata, headers, parameters, retryPolicy, addressSelector,
                addressSelectionContext, stat, Ticker.systemTicker());
    }

}
