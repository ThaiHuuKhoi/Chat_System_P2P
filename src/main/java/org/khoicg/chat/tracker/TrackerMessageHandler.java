package org.khoicg.chat.tracker;

import org.khoicg.chat.model.Message;

/**
 * Strategy for one tracker message type (OCP: add handler instead of editing a giant if-chain).
 */
public interface TrackerMessageHandler {

    boolean supports(String type);

    void handle(Message message, TrackerHandleContext ctx);
}
