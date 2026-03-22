package org.khoicg.chat.tracker.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.tracker.TrackerHandleContext;
import org.khoicg.chat.tracker.TrackerMessageHandler;

public final class HeartbeatTrackerHandler implements TrackerMessageHandler {

    @Override
    public boolean supports(String type) {
        return "HEARTBEAT".equals(type);
    }

    @Override
    public void handle(Message msg, TrackerHandleContext ctx) {
        ctx.reply(new Message("HEARTBEAT_OK", "Tracker", ""));
    }
}
