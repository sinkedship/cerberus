package com.sinkedship.cerberus.client.mimic.drift;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.airlift.drift.TApplicationException;
import io.airlift.drift.TException;
import io.airlift.drift.client.UncheckedTApplicationException;
import io.airlift.drift.client.UncheckedTException;
import io.airlift.drift.client.UncheckedTProtocolException;
import io.airlift.drift.client.UncheckedTTransportException;
import io.airlift.drift.protocol.TProtocolException;
import io.airlift.drift.protocol.TTransportException;
import io.airlift.drift.transport.client.DriftApplicationException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static io.airlift.drift.TApplicationException.Type.UNKNOWN_METHOD;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A completely mimic of io.airlift.drift.client.DriftInvocationHandler
 * excepts that it's public.
 *
 * @author Derrick Guan
 */
public class CerberusDriftInvocationHandler implements InvocationHandler {

    private static final Object[] NO_ARGS = new Object[0];

    private final String serviceName;
    private final Map<Method, CerberusDriftMethodHandler> methods;
    private final Optional<String> addressSelectionContext;
    private final Map<String, String> headers;

    public CerberusDriftInvocationHandler(String serviceName, Map<Method, CerberusDriftMethodHandler> methods,
                                          Optional<String> addressSelectionContext, Map<String, String> headers) {
        this.serviceName = requireNonNull(serviceName, "serviceName is null");
        this.methods = ImmutableMap.copyOf(requireNonNull(methods, "methods is null"));
        this.addressSelectionContext = requireNonNull(addressSelectionContext, "addressSelectionContext is null");
        this.headers = ImmutableMap.copyOf(requireNonNull(headers, "headers is null"));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            switch (method.getName()) {
                case "toString":
                    return serviceName;
                case "equals":
                    return Proxy.isProxyClass(args[0].getClass()) && (Proxy.getInvocationHandler(args[0]) == this);
                case "hashCode":
                    return System.identityHashCode(this);
            }
            throw new UnsupportedOperationException(method.getName());
        }

        if (args == null) {
            args = NO_ARGS;
        }

        if ((args.length == 0) && "close".equals(method.getName())) {
            return null;
        }

        CerberusDriftMethodHandler methodHandler = methods.get(method);

        try {
            if (methodHandler == null) {
                throw new TApplicationException(UNKNOWN_METHOD, "Unknown method: " + method);
            }

            ListenableFuture<Object> future = methodHandler.invoke(addressSelectionContext, headers, asList(args));

            if (methodHandler.isAsync()) {
                return unwrapUserException(future);
            }

            try {
                return future.get();
            } catch (ExecutionException e) {
                throw unwrapUserException(e.getCause());
            }
        } catch (Exception e) {
            // rethrow any exceptions declared to be thrown by the method
            boolean canThrowTException = false;
            for (Class<?> exceptionType : method.getExceptionTypes()) {
                if (exceptionType.isAssignableFrom(e.getClass())) {
                    throw e;
                }
                canThrowTException = canThrowTException || exceptionType == TException.class;
            }

            if (e instanceof TApplicationException) {
                throw new UncheckedTApplicationException((TApplicationException) e);
            }

            if (e instanceof TProtocolException) {
                throw new UncheckedTProtocolException((TProtocolException) e);
            }

            if (e instanceof TTransportException) {
                throw new UncheckedTTransportException((TTransportException) e);
            }

            if (e instanceof TException) {
                throw new UncheckedTException((TException) e);
            }

            TException wrappedException;
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                wrappedException = new TException("Thread interrupted", e);
            } else {
                wrappedException = new TException(e.getMessage(), e);
            }

            if (canThrowTException) {
                throw wrappedException;
            }
            throw new UncheckedTException(wrappedException);
        }
    }

    private static ListenableFuture<Object> unwrapUserException(ListenableFuture<Object> future) {
        SettableFuture<Object> result = SettableFuture.create();
        Futures.addCallback(future, new FutureCallback<Object>() {
                    @Override
                    public void onSuccess(Object value) {
                        result.set(value);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        result.setException(unwrapUserException(t));
                    }
                },
                directExecutor());
        return result;
    }

    private static Throwable unwrapUserException(Throwable t) {
        // unwrap de-serialized user exception
        return (t instanceof DriftApplicationException) ? t.getCause() : t;
    }
}
