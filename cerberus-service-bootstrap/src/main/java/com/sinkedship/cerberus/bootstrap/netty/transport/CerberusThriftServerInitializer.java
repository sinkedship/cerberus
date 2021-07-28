package com.sinkedship.cerberus.bootstrap.netty.transport;

import io.airlift.drift.transport.netty.server.OptionalSslHandler;
import io.airlift.drift.transport.netty.server.ThriftProtocolDetection;
import io.airlift.drift.transport.netty.server.ThriftServerHandler;
import io.airlift.drift.transport.server.ServerMethodInvoker;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

class CerberusThriftServerInitializer extends ChannelInitializer<SocketChannel> {

    private final ServerMethodInvoker methodInvoker;
    private final DataSize maxFrameSize;
    private final Duration requestTimeout;
    private final Optional<Supplier<SslContext>> sslContextSupplier;
    private final boolean allowPlainText;
    private final boolean assumeClientsSupportOutOfOrderResponses;

    private final EventExecutorGroup logicExecutorGroup;

    CerberusThriftServerInitializer(
            ServerMethodInvoker methodInvoker,
            DataSize maxFrameSize,
            Duration requestTimeout,
            Optional<Supplier<SslContext>> sslContextSupplier,
            boolean allowPlainText,
            boolean assumeClientsSupportOutOfOrderResponses,
            EventExecutorGroup logicExecutorGroup
    ) {
        requireNonNull(methodInvoker, "methodInvoker is null");
        requireNonNull(maxFrameSize, "maxFrameSize is null");
        requireNonNull(requestTimeout, "requestTimeout is null");
        requireNonNull(sslContextSupplier, "sslContextSupplier is null");
        checkArgument(allowPlainText || sslContextSupplier.isPresent(), "Plain text is not allowed, but SSL is not configured");
        requireNonNull(logicExecutorGroup, "logicExecutorGroup is null");

        this.methodInvoker = methodInvoker;
        this.maxFrameSize = maxFrameSize;
        this.requestTimeout = requestTimeout;
        this.sslContextSupplier = sslContextSupplier;
        this.allowPlainText = allowPlainText;
        this.assumeClientsSupportOutOfOrderResponses = assumeClientsSupportOutOfOrderResponses;
        this.logicExecutorGroup = logicExecutorGroup;
    }

    @Override
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        if (sslContextSupplier.isPresent()) {
            if (allowPlainText) {
                pipeline.addLast(new OptionalSslHandler(sslContextSupplier.get().get()));
            }
            else {
                pipeline.addLast(sslContextSupplier.get().get().newHandler(channel.alloc()));
            }
        }

        pipeline.addLast(new CerberusThriftProtocolDetection(
                new ThriftServerHandler(methodInvoker, requestTimeout, logicExecutorGroup),
                maxFrameSize,
                assumeClientsSupportOutOfOrderResponses,
                logicExecutorGroup));
    }
}
