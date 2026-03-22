package org.khoicg.chat.util;

import java.util.UUID;

public final class MessageIdUtil {
    private MessageIdUtil() {}

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
