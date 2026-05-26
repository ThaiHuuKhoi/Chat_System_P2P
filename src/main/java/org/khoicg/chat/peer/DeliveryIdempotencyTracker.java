package org.khoicg.chat.peer;

import org.khoicg.chat.model.Message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Drops duplicate application-level deliveries keyed by type|sender|messageId.
 * Uses an insertion-order LRU map: oldest entry is evicted when capacity is
 * exceeded, instead of clearing everything at once.
 */
public final class DeliveryIdempotencyTracker {

    private static final int MAX_KEYS = 4096;

    private final Map<String, Boolean> seen = Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>(MAX_KEYS + 1, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_KEYS;
                }
            });

    public boolean markFirstDelivery(Message msg) {
        String mid = msg.getMessageId();
        if (mid == null || mid.isEmpty()) return true;
        String key = msg.getType() + "|" + msg.getSenderId() + "|" + mid;
        synchronized (seen) {
            if (seen.containsKey(key)) return false;
            seen.put(key, Boolean.TRUE);
            return true;
        }
    }
}
