package org.khoicg.chat.tracker;

import org.khoicg.chat.model.Message;

/**
 * Removes peers that have not sent any message within {@code timeoutMs}.
 * Broadcasts PEER_LEFT to remaining peers after each removal.
 */
public final class TrackerStalePeerMonitor implements Runnable {

    private static final long CHECK_INTERVAL_MS = 10_000L;
    private static final long TIMEOUT_MS = 15_000L;

    private final TrackerState state;
    private final TrackerPushBroadcaster broadcaster;

    public TrackerStalePeerMonitor(TrackerState state, TrackerPushBroadcaster broadcaster) {
        this.state       = state;
        this.broadcaster = broadcaster;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(CHECK_INTERVAL_MS);
                long now = System.currentTimeMillis();
                for (String peerId : state.lastSeenSnapshot().keySet()) {
                    Long last = state.lastSeenSnapshot().get(peerId);
                    if (last != null && now - last > TIMEOUT_MS) {
                        state.removePeer(peerId);
                        System.out.println("[-] Phát hiện Peer rớt mạng (Timeout): Đã xóa " + peerId);
                        Message leftMsg = new Message("PEER_LEFT", "Tracker", peerId);
                        broadcaster.broadcast(leftMsg, null, state.onlinePeers());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
