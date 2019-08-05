package com.sinkedship.cerberus.bootstrap.netty.transport;

import io.airlift.drift.transport.netty.server.DriftNettyServerConfig;
import io.airlift.drift.transport.netty.server.ThriftServerInitializer;
import io.airlift.drift.transport.netty.ssl.SslContextFactory;
import io.airlift.drift.transport.server.ServerMethodInvoker;
import io.airlift.drift.transport.server.ServerTransport;
import com.sinkedship.cerberus.bootstrap.config.CerberusServerBootConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static io.airlift.concurrent.Threads.threadsNamed;
import static io.airlift.drift.transport.netty.ssl.SslContextFactory.createSslContextFactory;
import static io.netty.channel.ChannelOption.*;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A mimic implementation of {@link io.airlift.drift.transport.netty.server.DriftNettyServerTransport}
 * while makes a little difference that binds a local inet ipv4 address instead of an arbitrary local address.
 *
 * @author Derrick Guan
 */
public class CerberusNettyServerTransport implements ServerTransport {

    private final ServerBootstrap bootstrap;
    private final String host;
    private final int port;

    private final EventLoopGroup ioGroup;
    private final EventLoopGroup workerGroup;

    private Channel channel;

    private final AtomicBoolean running = new AtomicBoolean();

    public CerberusNettyServerTransport(ServerMethodInvoker methodInvoker, CerberusServerBootConfig configAdapter) {
        this(methodInvoker, configAdapter, ByteBufAllocator.DEFAULT);
    }

    public CerberusNettyServerTransport(ServerMethodInvoker methodInvoker, CerberusServerBootConfig configAdapter, ByteBufAllocator allocator) {
        requireNonNull(methodInvoker, "methodInvoker is null");
        requireNonNull(configAdapter, "config is null");
        this.port = configAdapter.getPort();
        this.host = configAdapter.getHost();
        DriftNettyServerConfig config = configAdapter.getUnderlyingConfig();

        ioGroup = new NioEventLoopGroup(configAdapter.getIoThreadCount(), threadsNamed("drift-server-io-%s"));

        workerGroup = new NioEventLoopGroup(configAdapter.getWorkerThreadCount(), threadsNamed("drift-server-worker-%s"));

        Optional<Supplier<SslContext>> sslContext = Optional.empty();
        if (config.isSslEnabled()) {
            SslContextFactory sslContextFactory = createSslContextFactory(false, config.getSslContextRefreshTime(), workerGroup);
            sslContext = Optional.of(sslContextFactory.get(
                    config.getTrustCertificate(),
                    Optional.ofNullable(config.getKey()),
                    Optional.ofNullable(config.getKey()),
                    Optional.ofNullable(config.getKeyPassword()),
                    config.getSessionCacheSize(),
                    config.getSessionTimeout(),
                    config.getCiphers()));

            // validate ssl context configuration is valid
            sslContext.get().get();
        }

        ThriftServerInitializer serverInitializer = new ThriftServerInitializer(
                methodInvoker,
                config.getMaxFrameSize(),
                config.getRequestTimeout(),
                sslContext,
                config.isAllowPlaintext(),
                config.isAssumeClientsSupportOutOfOrderResponses(),
                workerGroup);

        bootstrap = new ServerBootstrap()
                .group(ioGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(serverInitializer)
                .option(SO_BACKLOG, config.getAcceptBacklog())
                .option(ALLOCATOR, allocator)
                .childOption(SO_KEEPALIVE, true)
                .validate();
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            channel = bootstrap.bind(host, port).sync().channel();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted while starting", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (channel != null) {
                await(channel.close());
            }
        } finally {
            Future<?> ioShutdown;
            try {
                ioShutdown = ioGroup.shutdownGracefully(0, 0, SECONDS);
            } finally {
                await(workerGroup.shutdownGracefully(0, 0, SECONDS));
            }
            await(ioShutdown);
        }
    }

    public int getPort() {
        return ((InetSocketAddress) channel.localAddress()).getPort();
    }

    public String getHost() {
        return host;
    }

    private static void await(Future<?> future) {
        try {
            future.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
