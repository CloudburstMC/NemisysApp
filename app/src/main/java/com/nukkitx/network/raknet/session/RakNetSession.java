package com.nukkitx.network.raknet.session;

import com.nukkitx.nemisys.util.AndroidLogger;
import com.nukkitx.network.SessionConnection;
import com.nukkitx.network.raknet.RakNetPacket;
import com.nukkitx.network.raknet.RakNetServer;
import com.nukkitx.network.raknet.datagram.EncapsulatedRakNetPacket;
import com.nukkitx.network.raknet.datagram.RakNetDatagram;
import com.nukkitx.network.raknet.datagram.SentDatagram;
import com.nukkitx.network.raknet.enveloped.AddressedRakNetDatagram;
import com.nukkitx.network.raknet.enveloped.DirectAddressedRakNetPacket;
import com.nukkitx.network.raknet.util.IntRange;
import com.nukkitx.network.raknet.util.SplitPacketHelper;
import com.nukkitx.network.util.Preconditions;
import gnu.trove.map.TShortObjectMap;
import gnu.trove.map.hash.TShortObjectHashMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RakNetSession implements SessionConnection<RakNetPacket> {
    private static final int MAX_SPLIT_COUNT = 32;
    private final InetSocketAddress remoteAddress;
    private final short mtu;
    private final TShortObjectMap<SplitPacketHelper> splitPackets = new TShortObjectHashMap<>();
    private final AtomicInteger datagramSequenceGenerator = new AtomicInteger();
    private final AtomicInteger reliabilitySequenceGenerator = new AtomicInteger();
    private final AtomicInteger orderSequenceGenerator = new AtomicInteger();
    private final ConcurrentMap<Integer, SentDatagram> datagramAcks = new ConcurrentHashMap<>();
    private final Channel channel;
    private final RakNetServer server;
    private boolean closed = false;
    private boolean useOrdering = false;

    public RakNetSession(InetSocketAddress remoteAddress, short mtu, Channel channel, RakNetServer server) {
        this.remoteAddress = remoteAddress;
        this.mtu = mtu;
        this.channel = channel;
        this.server = server;
    }

    public Optional<InetSocketAddress> getRemoteAddress() {
        return Optional.of(remoteAddress);
    }

    public short getMtu() {
        return mtu;
    }

    public AtomicInteger getDatagramSequenceGenerator() {
        return datagramSequenceGenerator;
    }

    public AtomicInteger getReliabilitySequenceGenerator() {
        return reliabilitySequenceGenerator;
    }

    public AtomicInteger getOrderSequenceGenerator() {
        return orderSequenceGenerator;
    }

    public Optional<ByteBuf> addSplitPacket(EncapsulatedRakNetPacket packet) {
        checkForClosed();

        SplitPacketHelper helper;
        synchronized (splitPackets) {
            // Make sure that we don't exceed a reasonable number of outstanding total split packets.
            if (splitPackets.size() >= MAX_SPLIT_COUNT) {
                if (!splitPackets.containsKey(packet.getPartId())) {
                    throw new IllegalStateException("Too many outstanding split packets");
                }
            }

            helper = splitPackets.get(packet.getPartId());
            if (helper == null) {
                splitPackets.put(packet.getPartId(), helper = new SplitPacketHelper(packet.getPartCount()));
            }

            // Retain the packet so it can be reassembled later.
            packet.retain();

            // Try reassembling the packet.
            Optional<ByteBuf> result = helper.add(packet);
            if (result.isPresent()) {
                splitPackets.remove(packet.getPartId());
            }

            return result;
        }
    }

    public void onAck(List<IntRange> acked) {
        checkForClosed();
        for (IntRange range : acked) {
            for (int i = range.getStart(); i <= range.getEnd(); i++) {
                SentDatagram datagram = datagramAcks.remove(i);
                if (datagram != null) {
                    datagram.tryRelease();
                }
            }
        }
    }

    public void onNak(List<IntRange> acked) {
        checkForClosed();
        for (IntRange range : acked) {
            for (int i = range.getStart(); i <= range.getEnd(); i++) {
                SentDatagram datagram = datagramAcks.get(i);
                if (datagram != null) {
                    AndroidLogger.debug("Resending datagram " + datagram.getDatagram().getDatagramSequenceNumber() + " due to NAK");
                    datagram.refreshForResend();
                    channel.write(new AddressedRakNetDatagram(datagram.getDatagram(), remoteAddress), channel.voidPromise());
                }
            }
        }

        channel.flush();
    }

    protected void internalSendRakNetPackage(ByteBuf encoded) {
        List<EncapsulatedRakNetPacket> addressed = EncapsulatedRakNetPacket.encapsulatePackage(encoded, this, useOrdering);
        List<RakNetDatagram> datagrams = new ArrayList<>();
        for (EncapsulatedRakNetPacket packet : addressed) {
            RakNetDatagram datagram = new RakNetDatagram();
            datagram.setDatagramSequenceNumber(datagramSequenceGenerator.getAndIncrement());
            if (!datagram.tryAddPacket(packet, mtu)) {
                throw new IllegalArgumentException("Packet too large to fit in MTU (size: " + packet.totalLength() + ", MTU: " + mtu + ")");
            }
            datagrams.add(datagram.retain()); // retain in case we need to resend it
        }

        for (RakNetDatagram netDatagram : datagrams) {
            channel.write(new AddressedRakNetDatagram(netDatagram, remoteAddress), channel.voidPromise());
            datagramAcks.put(netDatagram.getDatagramSequenceNumber(), new SentDatagram(netDatagram));
        }
        channel.flush();
    }

    public void sendDirectPackage(RakNetPacket netPackage) {
        checkForClosed();
        channel.writeAndFlush(new DirectAddressedRakNetPacket(netPackage, remoteAddress), channel.voidPromise());
    }

    public void onTick() {
        if (closed) {
            return;
        }

        resendStalePackets();
        cleanSplitPackets();
    }

    private void cleanSplitPackets() {
        synchronized (splitPackets) {
            for (Iterator<SplitPacketHelper> it = splitPackets.valueCollection().iterator(); it.hasNext(); ) {
                SplitPacketHelper sph = it.next();
                if (sph.expired()) {
                    sph.release();
                    it.remove();
                }
            }
        }
    }

    private void resendStalePackets() {
        for (SentDatagram datagram : datagramAcks.values()) {
            if (datagram.isStale()) {
                AndroidLogger.debug("Resending datagram " + datagram.getDatagram().getDatagramSequenceNumber() + " due to being stale");
                datagram.refreshForResend();
                channel.write(new AddressedRakNetDatagram(datagram.getDatagram(), remoteAddress), channel.voidPromise());
            }
        }
        channel.flush();
    }

    public void close() {
        checkForClosed();
        closed = true;

        // Perform resource clean up.
        synchronized (splitPackets) {
            splitPackets.forEachValue(v -> {
                v.release();
                return true;
            });
            splitPackets.clear();
        }

        datagramAcks.values().forEach(SentDatagram::tryRelease);
        datagramAcks.clear();
    }

    @Override
    public void sendPacket(@Nonnull RakNetPacket packet) {
        internalSendRakNetPackage(server.getPacketRegistry().tryEncode(packet));
    }

    void checkForClosed() {
        Preconditions.checkState(!closed, "Session already closed");
    }

    public boolean isClosed() {
        return closed;
    }

    public RakNetServer getServer() {
        return server;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isUseOrdering() {
        return useOrdering;
    }

    public void setUseOrdering(boolean useOrdering) {
        this.useOrdering = useOrdering;
    }
}
