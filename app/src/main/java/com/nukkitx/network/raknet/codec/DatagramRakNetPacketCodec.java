package com.nukkitx.network.raknet.codec;

import com.nukkitx.network.raknet.RakNetPacket;
import com.nukkitx.network.raknet.RakNetPacketRegistry;
import com.nukkitx.network.raknet.RakNetServer;
import com.nukkitx.network.raknet.datagram.RakNetDatagramFlags;
import com.nukkitx.network.raknet.enveloped.DirectAddressedRakNetPacket;
import com.nukkitx.network.raknet.packet.AckPacket;
import com.nukkitx.network.raknet.packet.NakPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

public class DatagramRakNetPacketCodec extends MessageToMessageCodec<DatagramPacket, DirectAddressedRakNetPacket> {
    private static final int USER_ID_START = 0x80;
    private final RakNetPacketRegistry registry;

    public DatagramRakNetPacketCodec(RakNetServer server) {
        this.registry = server.getPacketRegistry();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, DirectAddressedRakNetPacket packet, List<Object> list) throws Exception {
        // Certain RakNet packets do not require special encapsulation. This encoder tries to handle them.
        try {
            ByteBuf buf = registry.tryEncode(packet.content());
            list.add(new DatagramPacket(buf, packet.recipient(), packet.sender()));
        } finally {
            packet.release();
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> list) throws Exception {
        // Certain RakNet packets do not require special encapsulation. This encoder tries to handle them.
        ByteBuf buf = packet.content();
        if (buf.readableBytes() == 0) {
            // not interested
            return;
        }

        buf.markReaderIndex();

        int id = buf.readUnsignedByte();
        if (id < USER_ID_START) { // User data
            buf.resetReaderIndex();

            // We can decode a packet immediately.
            RakNetPacket rakNetPacket = registry.tryDecode(buf);
            if (rakNetPacket != null) {
                list.add(new DirectAddressedRakNetPacket(rakNetPacket, packet.recipient(), packet.sender()));
            }
        } else {
            // We can decode some datagrams directly.
            buf.resetReaderIndex();
            RakNetDatagramFlags flags = new RakNetDatagramFlags(buf.readByte());
            if (flags.isValid()) {
                if (flags.isAck()) {
                    // ACK
                    AckPacket ackPacket = new AckPacket();
                    ackPacket.decode(buf);
                    list.add(new DirectAddressedRakNetPacket(ackPacket, packet.recipient(), packet.sender()));
                } else if (flags.isNak()) {
                    // NAK
                    NakPacket nakPacket = new NakPacket();
                    nakPacket.decode(buf);
                    list.add(new DirectAddressedRakNetPacket(nakPacket, packet.recipient(), packet.sender()));
                } else {
                    buf.readerIndex(0);
                    list.add(packet.retain()); // needs further processing
                }
            } else {
                buf.readerIndex(0); // not interested
            }
        }
    }
}
