package org.khoicg.chat.tracker.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.tracker.TrackerHandleContext;
import org.khoicg.chat.tracker.TrackerMessageHandler;

public final class ChordFingerTrackerHandler implements TrackerMessageHandler {

    @Override
    public boolean supports(String type) {
        return "CHORD_FINGER".equals(type);
    }

    @Override
    public void handle(Message msg, TrackerHandleContext ctx) {
        String pid = (msg.getContent() != null && !msg.getContent().isBlank())
                ? msg.getContent().trim()
                : msg.getSenderId();
        ctx.reply(new Message("CHORD_DATA", "Tracker",
                ctx.gson().toJson(ctx.state().chord().fingerViewFor(pid))));
    }
}
