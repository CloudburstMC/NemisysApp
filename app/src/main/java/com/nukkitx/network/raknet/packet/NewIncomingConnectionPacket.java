package com.nukkitx.network.raknet.packet;

import com.nukkitx.network.raknet.RakNetPacket;
import com.nukkitx.network.raknet.RakNetUtil;
import io.netty.buffer.ByteBuf;
import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class NewIncomingConnectionPacket implements RakNetPacket {
    private InetSocketAddress clientAddress;
    private InetSocketAddress[] systemAddresses;
    private long clientTimestamp;
    private long serverTimestamp;

    @Override
    public void encode(ByteBuf buffer) {
        RakNetUtil.writeAddress(buffer, clientAddress);
        for (InetSocketAddress address : systemAddresses) {
            RakNetUtil.writeAddress(buffer, address);
        }
        buffer.writeLong(clientTimestamp);
        buffer.writeLong(serverTimestamp);
    }

    @Override
    public void decode(ByteBuf buffer) {
        clientAddress = RakNetUtil.readAddress(buffer);
        systemAddresses = new InetSocketAddress[20];
        for (int i = 0; i < 20; i++) {
            systemAddresses[i] = RakNetUtil.readAddress(buffer);
        }
        clientTimestamp = buffer.readLong();
        serverTimestamp = buffer.readLong();
    }
}
