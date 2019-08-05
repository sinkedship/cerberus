package com.sinkedship.cerberus.bootstrap.netty.transport;

import io.airlift.drift.transport.server.ServerMethodInvoker;
import io.airlift.drift.transport.server.ServerTransport;
import io.airlift.drift.transport.server.ServerTransportFactory;
import com.sinkedship.cerberus.bootstrap.config.CerberusServerBootConfig;
import io.netty.buffer.ByteBufAllocator;

/**
 * A implementation of {@link ServerTransportFactory} which
 * creates a concrete implementation {@link CerberusNettyServerTransport} of {@link ServerTransport}.
 *
 * @author Derrick Guan
 */
public class CerberusNettyServerTransportFactory implements ServerTransportFactory {

    private final CerberusServerBootConfig configAdapter;

    private final ByteBufAllocator allocator;

    public CerberusNettyServerTransportFactory(CerberusServerBootConfig configAdapter) {
        this(configAdapter, ByteBufAllocator.DEFAULT);
    }

    public CerberusNettyServerTransportFactory(CerberusServerBootConfig configAdapter,
                                               ByteBufAllocator allocator) {
        this.configAdapter = configAdapter;
        this.allocator = allocator;
    }

    @Override
    public ServerTransport createServerTransport(ServerMethodInvoker serverMethodInvoker) {
        return new CerberusNettyServerTransport(serverMethodInvoker, configAdapter, allocator);
    }
}
