package org.khoicg.chat.tracker;

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

    private static final int OFFLINE_QUEUE_LIMIT = 50;

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

    public boolean storeOfflineFor(String targetId, Message storedMsg) {
        List<Message> queue = offlineMessages.computeIfAbsent(
                targetId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (queue) {
            if (queue.size() >= OFFLINE_QUEUE_LIMIT) return false;
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
