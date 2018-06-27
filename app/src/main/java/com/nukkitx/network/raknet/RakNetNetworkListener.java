package com.nukkitx.network.raknet;

import com.nukkitx.nemisys.util.AndroidLogger;
import com.nukkitx.network.NativeUtils;
import com.nukkitx.network.NetworkListener;
import com.nukkitx.network.NetworkSession;
import com.nukkitx.network.raknet.codec.DatagramRakNetDatagramCodec;
import com.nukkitx.network.raknet.codec.DatagramRakNetPacketCodec;
import com.nukkitx.network.raknet.handler.ExceptionHandler;
import com.nukkitx.network.raknet.handler.RakNetDatagramHandler;
import com.nukkitx.network.raknet.handler.RakNetPacketHandler;
import com.nukkitx.network.util.NetworkThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;

public class RakNetNetworkListener<T extends NetworkSession> extends ChannelInitializer<DatagramChannel> implements NetworkListener {
    private final RakNetServer<T> server;
    private final InetSocketAddress address;
    private final Bootstrap bootstrap;
    private final int maxThreads;
    private DatagramChannel channel;

    RakNetNetworkListener(RakNetServer<T> server, InetSocketAddress address, int maxThreads) {
        this.server = server;
        this.address = address;
        this.maxThreads = maxThreads;

        ThreadFactory listenerThreadFactory = NetworkThreadFactory.builder().format("RakNet Listener - #%d")
                .daemon(true).build();
        bootstrap = new Bootstrap().option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).handler(this);

        bootstrap.channel(NioDatagramChannel.class)
                    .group(new NioEventLoopGroup(0, listenerThreadFactory));
    }

    @Override
    public boolean bind() {
        int threads = NativeUtils.isReusePortAvailable() ? maxThreads : 1;

        boolean success = false;

        for (int i = 0; i < threads; i++) {
            try {
                ChannelFuture future = bootstrap.bind(address).await();
                if (future.isSuccess()) {
                    AndroidLogger.debug("Bound RakNet listener #" + i + " to " + address);
                    success = true;
                } else {
                    AndroidLogger.error("Was unable to bind RakNet listener #" + address, future.cause());
                }
            } catch (InterruptedException e) {
                AndroidLogger.warn("Interrupted whilst binding RakNet listener #" + i);
            }
        }
        return success;
    }

    @Override
    public void close() {
        bootstrap.config().group().shutdownGracefully();
        if (channel != null) {
            channel.close().syncUninterruptibly();
        }
    }

    @Override
    protected void initChannel(DatagramChannel datagramChannel) throws Exception {
        this.channel = datagramChannel;
        channel.pipeline()
                .addLast("datagramRakNetPacketCodec", new DatagramRakNetPacketCodec(server))
                .addLast("raknetPacketHandler", new RakNetPacketHandler(server))
                .addLast("datagramRakNetDatagramCodec", new DatagramRakNetDatagramCodec(server))
                .addLast("raknetDatagramHandler", new RakNetDatagramHandler<>(server))
                .addLast("exceptionHandler", new ExceptionHandler());
    }

    @Override
    public InetSocketAddress getAddress() {
        return null;
    }
}
