package com.nukkitx.nemisys;

import com.nukkitx.network.raknet.RakNetEventListener;

import java.net.InetSocketAddress;

public class AndroidEventListener implements RakNetEventListener {

    @Override
    public Action onConnectionRequest(InetSocketAddress inetSocketAddress) {
        return Action.NO_INCOMING_CONNECTIONS;
    }

    @Override
    public Advertisement onQuery(InetSocketAddress inetSocketAddress) {
        return new Advertisement(
                "MCPE",
                "RakNetAndroid",
                280,
                "1.4.2",
                0,
                5,
                "SUBMOTD",
                "Creative"
        );
    }
}
