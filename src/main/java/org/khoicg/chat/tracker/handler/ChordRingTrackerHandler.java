package org.khoicg.chat.tracker.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.tracker.TrackerHandleContext;
import org.khoicg.chat.tracker.TrackerMessageHandler;

public final class ChordRingTrackerHandler implements TrackerMessageHandler {

    @Override
    public boolean supports(String type) {
        return "CHORD_RING".equals(type);
    }

    @Override
    public void handle(Message msg, TrackerHandleContext ctx) {
        ctx.reply(new Message("CHORD_DATA", "Tracker",
                ctx.gson().toJson(ctx.state().chord().snapshot())));
    }
}
