package org.khoicg.chat.util;

import com.google.gson.Gson;
import org.khoicg.chat.model.Message;

public final class ReliableDeliveryHelper {
    private static final Gson GSON = new Gson();

    private ReliableDeliveryHelper() {}

    public static boolean ackJsonMatches(String jsonLine, String expectedMessageId) {
        if (jsonLine == null) return false;
        try {
            Message m = GSON.fromJson(jsonLine, Message.class);
            if (m == null || !"ACK".equals(m.getType())) return false;
            if (expectedMessageId == null || expectedMessageId.isEmpty()) return true;
            return expectedMessageId.equals(m.getMessageId());
        } catch (Exception e) {
            return false;
        }
    }
}
