package org.khoicg.chat.tracker.handler;

import org.khoicg.chat.chord.ChordRing;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.tracker.TrackerHandleContext;
import org.khoicg.chat.tracker.TrackerMessageHandler;

public final class ChordLookupTrackerHandler implements TrackerMessageHandler {

    @Override
    public boolean supports(String type) {
        return "CHORD_LOOKUP".equals(type);
    }

    @Override
    public void handle(Message msg, TrackerHandleContext ctx) {
        String key = msg.getContent() != null ? msg.getContent() : "";
        ChordRing.LookupView lv = ctx.state().chord().lookup(key);
        ctx.reply(new Message("CHORD_DATA", "Tracker", ctx.gson().toJson(lv)));
    }
}
