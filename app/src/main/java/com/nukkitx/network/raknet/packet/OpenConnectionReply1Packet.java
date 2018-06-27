package com.nukkitx.network.raknet.packet;

import com.nukkitx.network.raknet.RakNetPacket;
import com.nukkitx.network.raknet.RakNetUtil;
import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public class OpenConnectionReply1Packet implements RakNetPacket {
    private long serverId;
    private boolean serverSecurity;
    private int mtuSize;

    @Override
    public void encode(ByteBuf buffer) {
        RakNetUtil.writeUnconnectedMagic(buffer);
        buffer.writeLong(serverId);
        buffer.writeByte((serverSecurity ? 1 : 0));
        buffer.writeShort(mtuSize);
    }

    @Override
    public void decode(ByteBuf buffer) {
        RakNetUtil.verifyUnconnectedMagic(buffer);
        serverId = buffer.readLong();
        serverSecurity = (buffer.readByte() != 0);
        mtuSize = buffer.readShort();
    }
}
