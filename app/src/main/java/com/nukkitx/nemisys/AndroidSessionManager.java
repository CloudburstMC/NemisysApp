package com.nukkitx.nemisys;

import com.nukkitx.network.NetworkSession;
import com.nukkitx.network.SessionManager;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AndroidSessionManager implements SessionManager {

    private static final int SESSIONS_PER_THREAD = 50;

    private final ConcurrentMap<InetSocketAddress, AndroidSession> sessions = new ConcurrentHashMap<>();

    public boolean add(InetSocketAddress address, AndroidSession session) {
        boolean added = sessions.putIfAbsent(address, session) == null;
        return added;
    }

    public boolean remove(AndroidSession session) {
        boolean removed = sessions.values().remove(session);
        return removed;
    }

    public AndroidSession get(InetSocketAddress address) {
        return sessions.get(address);
    }

    public Collection<AndroidSession> all() {
        return new ArrayList<>(sessions.values());
    }

    @Override
    public int getCount() {
        return sessions.size();
    }

    public boolean add(AndroidSession session) {
        return true;
    }

    public void onTick() {
        for (AndroidSession session : sessions.values()) {
            session.onTick();
        }
    }

    @Override
    public boolean remove(NetworkSession networkSession) {
        return true;
    }

    @Override
    public boolean add(InetSocketAddress inetSocketAddress, NetworkSession networkSession) {
        return true;
    }
}
