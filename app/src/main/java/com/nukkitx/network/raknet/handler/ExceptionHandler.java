package com.nukkitx.network.raknet.handler;

import com.nukkitx.nemisys.util.AndroidLogger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ExceptionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        AndroidLogger.error("Exception occurred whilst handling packet", cause);
    }
}
