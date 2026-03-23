package org.khoicg.chat.tracker;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable tracker state (SRP). Handlers mutate only through this type.
 */
public final class TrackerState {

    private final ConcurrentHashMap<String, PeerInfo> onlinePeers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastSeenPeers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Message>> offlineMessages = new ConcurrentHashMap<>();

    public void touch(String senderId) {
        if (senderId != null) {
            lastSeenPeers.put(senderId, System.currentTimeMillis());
        }
    }

    public void registerPeer(PeerInfo newPeer) {
        onlinePeers.put(newPeer.getPeerId(), newPeer);
        lastSeenPeers.put(newPeer.getPeerId(), System.currentTimeMillis());
    }

    public void removePeer(String peerId) {
        if (peerId == null) {
            return;
        }
        onlinePeers.remove(peerId);
        lastSeenPeers.remove(peerId);
    }

    public Collection<PeerInfo> onlinePeers() {
        return onlinePeers.values();
    }

    public void storeOfflineFor(String targetId, Message storedMsg) {
        offlineMessages.computeIfAbsent(targetId, k -> new ArrayList<>()).add(storedMsg);
    }

    public List<Message> takeOfflineQueue(String peerId) {
        return offlineMessages.remove(peerId);
    }

    public ConcurrentHashMap<String, Long> lastSeenSnapshot() {
        return lastSeenPeers;
    }
}
