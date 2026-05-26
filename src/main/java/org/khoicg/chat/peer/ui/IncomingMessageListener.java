package org.khoicg.chat.peer.ui;

@FunctionalInterface
public interface IncomingMessageListener {
    void onEvent(String eventType, String senderId, String content);
}
