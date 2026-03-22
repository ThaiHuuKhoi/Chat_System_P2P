package org.khoicg.chat.peer;

import org.khoicg.chat.model.Message;

import java.util.List;

public final class PeerMessageDispatcher {

    private final List<PeerInboundHandler> handlers;

    public PeerMessageDispatcher(List<PeerInboundHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public void dispatch(Message message, PeerHandleContext ctx) {
        if (message == null || message.getType() == null) {
            return;
        }
        for (PeerInboundHandler h : handlers) {
            if (h.supports(message.getType())) {
                h.handle(message, ctx);
                return;
            }
        }
    }
}
