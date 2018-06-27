package com.nukkitx.network.raknet.packet;

import com.nukkitx.network.raknet.RakNetPacket;
import com.nukkitx.network.raknet.RakNetUtil;
import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public class NoFreeIncomingConnectionsPacket implements RakNetPacket {
    private long serverId;

    @Override
    public void encode(ByteBuf buffer) {
        RakNetUtil.writeUnconnectedMagic(buffer);
        buffer.writeLong(serverId);
    }

    @Override
    public void decode(ByteBuf buffer) {
        RakNetUtil.verifyUnconnectedMagic(buffer);
        serverId = buffer.readLong();
    }
}
