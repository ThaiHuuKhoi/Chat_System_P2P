package org.khoicg.chat.tracker;

import org.khoicg.chat.model.Message;

import java.util.List;

public final class TrackerMessageDispatcher {

    private final List<TrackerMessageHandler> handlers;

    public TrackerMessageDispatcher(List<TrackerMessageHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public void dispatch(Message message, TrackerHandleContext ctx) {
        if (message == null || message.getType() == null) {
            return;
        }
        String type = message.getType();
        for (TrackerMessageHandler h : handlers) {
            if (h.supports(type)) {
                h.handle(message, ctx);
                return;
            }
        }
    }
}
