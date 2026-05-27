package org.khoicg.chat.tracker;

import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable tracker state (SRP). Handlers mutate only through this type.
 */
public final class TrackerState {


    private final ConcurrentHashMap<String, PeerInfo> onlinePeers  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PeerInfo> knownPeers   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long>     lastSeenPeers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Message>> offlineMessages = new ConcurrentHashMap<>();

    public void touch(String senderId) {
        if (senderId != null) {
            lastSeenPeers.put(senderId, System.currentTimeMillis());
        }
    }

    public void registerPeer(PeerInfo newPeer) {
        onlinePeers.put(newPeer.getPeerId(), newPeer);
        knownPeers.put(newPeer.getPeerId(), newPeer);
        lastSeenPeers.put(newPeer.getPeerId(), System.currentTimeMillis());
    }

    public void removePeer(String peerId) {
        if (peerId == null) {
            return;
        }
        onlinePeers.remove(peerId);
        lastSeenPeers.remove(peerId);
        // knownPeers intentionally kept — needed for offline message routing
    }

    public PeerInfo findKnownPeerById(String peerId) {
        return peerId == null ? null : knownPeers.get(peerId);
    }

    /** Tìm peerId của peer đã từng đăng ký theo địa chỉ IP:port. */
    public String findKnownPeerByAddress(String ip, int port) {
        String normalized = normalizeHost(ip);
        for (PeerInfo p : knownPeers.values()) {
            if (p.getPort() == port && normalizeHost(p.getIpAddress()).equals(normalized)) {
                return p.getPeerId();
            }
        }
        return null;
    }

    private static String normalizeHost(String host) {
        if (host == null) return "";
        String h = host.trim().toLowerCase();
        return "127.0.0.1".equals(h) ? "localhost" : h;
    }

    public Collection<PeerInfo> onlinePeers() {
        return onlinePeers.values();
    }

    public Collection<PeerInfo> knownPeers() {
        return knownPeers.values();
    }

    public boolean storeOfflineFor(String targetId, Message storedMsg) {
        List<Message> queue = offlineMessages.computeIfAbsent(
                targetId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (queue) {
            if (queue.size() >= AppConfig.trackerOfflineQueueLimit()) return false;
            queue.add(storedMsg);
            return true;
        }
    }

    public List<Message> takeOfflineQueue(String peerId) {
        return offlineMessages.remove(peerId);
    }

    public Map<String, Long> lastSeenSnapshot() {
        return Collections.unmodifiableMap(lastSeenPeers);
    }
}
