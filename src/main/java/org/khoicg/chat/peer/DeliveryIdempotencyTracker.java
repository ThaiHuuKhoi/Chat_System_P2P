package org.khoicg.chat.peer;

import org.khoicg.chat.model.Message;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Drops duplicate application-level deliveries keyed by type|sender|messageId (SRP).
 */
public final class DeliveryIdempotencyTracker {

    private static final int MAX_KEYS = 4096;

    private final ConcurrentHashMap<String, Boolean> seen = new ConcurrentHashMap<>();

    public boolean markFirstDelivery(Message msg) {
        String mid = msg.getMessageId();
        if (mid == null || mid.isEmpty()) {
            return true;
        }
        String key = msg.getType() + "|" + msg.getSenderId() + "|" + mid;
        if (seen.size() > MAX_KEYS) {
            seen.clear();
        }
        return seen.putIfAbsent(key, Boolean.TRUE) == null;
    }
}
