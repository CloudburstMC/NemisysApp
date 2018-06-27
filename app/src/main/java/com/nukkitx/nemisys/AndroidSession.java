package com.nukkitx.nemisys;

import com.nukkitx.network.NetworkSession;
import com.nukkitx.network.SessionConnection;
import com.nukkitx.network.raknet.RakNetPacket;
import com.nukkitx.network.util.Preconditions;

public class AndroidSession implements NetworkSession<RakNetPacket> {

    private SessionConnection<RakNetPacket> connection;

    public AndroidSession(SessionConnection<RakNetPacket> connection) {
        this.connection = connection;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public SessionConnection<RakNetPacket> getConnection() {
        return connection;
    }

    @Override
    public void onTick() {
        connection.onTick();
    }

    @Override
    public void touch() {
        Preconditions.checkState(!connection.isClosed(), "Connection has been closed!");
    }
}
