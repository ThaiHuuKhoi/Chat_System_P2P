package org.khoicg.chat.peer;

import org.khoicg.chat.model.Message;

public interface PeerInboundHandler {

    boolean supports(String type);

    void handle(Message message, PeerHandleContext ctx);
}
