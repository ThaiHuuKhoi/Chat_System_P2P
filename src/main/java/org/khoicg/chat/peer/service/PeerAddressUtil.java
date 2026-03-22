package org.khoicg.chat.peer.service;

import org.khoicg.chat.model.PeerInfo;

import java.util.List;

public final class PeerAddressUtil {

    private PeerAddressUtil() {}

    public static String resolvePeerIdByAddress(List<PeerInfo> peers, String ip, int port) {
        if (peers == null) {
            return null;
        }
        String n = normalizeHost(ip);
        for (PeerInfo p : peers) {
            if (p.getPort() != port) {
                continue;
            }
            if (normalizeHost(p.getIpAddress()).equals(n)) {
                return p.getPeerId();
            }
        }
        return null;
    }

    public static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String h = host.trim().toLowerCase();
        if ("127.0.0.1".equals(h)) {
            return "localhost";
        }
        return h;
    }
}
