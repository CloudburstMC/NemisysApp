package com.nukkitx.nemisys;

import com.nukkitx.network.raknet.CustomRakNetPacket;

import io.netty.buffer.ByteBuf;

public class AndroidPacket implements CustomRakNetPacket<AndroidSession> {

    private boolean encrypted;
    private ByteBuf batched;
    private ByteBuf payload;

    @Override
    public void encode(ByteBuf buffer) {
        buffer.writeBytes(payload);
        payload.release();
    }

    @Override
    public void decode(ByteBuf buffer) {
        payload = buffer.readBytes(buffer.readableBytes());
    }

    @Override
    public void handle(AndroidSession session) throws Exception {

    }
}
