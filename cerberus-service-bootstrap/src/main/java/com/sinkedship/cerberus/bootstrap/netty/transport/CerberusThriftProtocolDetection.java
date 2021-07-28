package com.sinkedship.cerberus.bootstrap.netty.transport;

import com.google.common.primitives.Ints;
import io.airlift.drift.transport.netty.codec.Protocol;
import io.airlift.drift.transport.netty.codec.Transport;
import io.airlift.drift.transport.netty.server.ResponseOrderingHandler;
import io.airlift.drift.transport.netty.server.ThriftServerHandler;
import io.airlift.units.DataSize;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.List;
import java.util.Optional;

import static io.airlift.drift.transport.netty.codec.Protocol.*;
import static io.airlift.drift.transport.netty.codec.Transport.*;
import static java.util.Objects.requireNonNull;

public class CerberusThriftProtocolDetection extends ByteToMessageDecoder {

    private static final int UNFRAMED_MESSAGE_FLAG = 0x8000_0000;
    private static final int UNFRAMED_MESSAGE_MASK = 0x8000_0000;

    private static final int BINARY_PROTOCOL_VERSION_MASK = 0xFFFF_0000;
    private static final int BINARY_PROTOCOL_VERSION_1 = 0x8001_0000;

    private static final int COMPACT_PROTOCOL_VERSION_MASK = 0xFF1F_0000;
    private static final int COMPACT_PROTOCOL_VERSION_1 = 0x8201_0000;
    private static final int COMPACT_PROTOCOL_VERSION_2 = 0x8202_0000;

    // 16th and 32nd bits must be 0 to differentiate framed vs unframed.
    private static final int HEADER_MAGIC = 0x0FFF_0000;
    private static final int HEADER_MAGIC_MASK = 0xFFFF_0000;

    private static final int HTTP_POST_MAGIC = Ints.fromBytes((byte) 'P', (byte) 'O', (byte) 'S', (byte) 'T');

    private final ThriftServerHandler thriftServerHandler;
    private final DataSize maxFrameSize;
    private final boolean assumeClientsSupportOutOfOrderResponses;
    private final EventExecutorGroup logicExecutorGroup;

    public CerberusThriftProtocolDetection(ThriftServerHandler thriftServerHandler, DataSize maxFrameSize,
                                           boolean assumeClientsSupportOutOfOrderResponses,
                                           EventExecutorGroup logicExecutorGroup) {
        this.maxFrameSize = requireNonNull(maxFrameSize, "maxFrameSize is null");
        this.thriftServerHandler = requireNonNull(thriftServerHandler, "thriftServerHandler is null");
        this.assumeClientsSupportOutOfOrderResponses = assumeClientsSupportOutOfOrderResponses;
        this.logicExecutorGroup = requireNonNull(logicExecutorGroup, "logicExecutorGroup is null");;
    }

    // This method is an exception to the normal reference counted rules and buffer should not be released
    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out)
    {
        // minimum bytes required to detect framing
        if (in.readableBytes() < 4) {
            return;
        }

        int magic = in.getInt(in.readerIndex());

        // HTTP not supported
        if (magic == HTTP_POST_MAGIC) {
            in.clear();
            context.close();
            return;
        }

        // Unframed transport magic starts with the high byte set, whereas framed and header
        // both start with the frame size which must be a positive int
        if ((magic & UNFRAMED_MESSAGE_MASK) == UNFRAMED_MESSAGE_FLAG) {
            Optional<Protocol> protocol = detectProtocol(magic);
            if (!protocol.isPresent()) {
                in.clear();
                context.close();
                return;
            }

            switchToTransport(context, UNFRAMED, protocol);
            return;
        }

        // The second int is used to determine if the transport is framed or header
        if (in.readableBytes() < 8) {
            return;
        }

        int magic2 = in.getInt(in.readerIndex() + 4);
        if ((magic2 & HEADER_MAGIC_MASK) == HEADER_MAGIC) {
            switchToTransport(context, HEADER, Optional.empty());
            return;
        }

        Optional<Protocol> protocol = detectProtocol(magic2);
        if (!protocol.isPresent()) {
            in.clear();
            context.close();
            return;
        }

        switchToTransport(context, FRAMED, protocol);
    }

    private static Optional<Protocol> detectProtocol(int magic)
    {
        if ((magic & BINARY_PROTOCOL_VERSION_MASK) == BINARY_PROTOCOL_VERSION_1) {
            return Optional.of(BINARY);
        }
        if ((magic & COMPACT_PROTOCOL_VERSION_MASK) == COMPACT_PROTOCOL_VERSION_1) {
            return Optional.of(COMPACT);
        }
        if ((magic & COMPACT_PROTOCOL_VERSION_MASK) == COMPACT_PROTOCOL_VERSION_2) {
            return Optional.of(FB_COMPACT);
        }
        return Optional.empty();
    }

    private void switchToTransport(ChannelHandlerContext context, Transport transport, Optional<Protocol> protocol)
    {
        ChannelPipeline pipeline = context.pipeline();
        transport.addFrameHandlers(pipeline, protocol, maxFrameSize, assumeClientsSupportOutOfOrderResponses);
        pipeline.addLast(new ResponseOrderingHandler());
        // use separate executor loop group to execute actual call
        pipeline.addLast(logicExecutorGroup, thriftServerHandler);

        // remove(this) must be last because it triggers downstream processing of the current message
        pipeline.remove(this);
    }
}
