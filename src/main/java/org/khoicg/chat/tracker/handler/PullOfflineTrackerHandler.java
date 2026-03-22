package org.khoicg.chat.tracker.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.tracker.TrackerHandleContext;
import org.khoicg.chat.tracker.TrackerMessageHandler;

import java.util.List;

public final class PullOfflineTrackerHandler implements TrackerMessageHandler {

    @Override
    public boolean supports(String type) {
        return "PULL_OFFLINE".equals(type);
    }

    @Override
    public void handle(Message msg, TrackerHandleContext ctx) {
        List<Message> msgs = ctx.state().takeOfflineQueue(msg.getSenderId());
        if (msgs != null && !msgs.isEmpty()) {
            ctx.reply(new Message("OFFLINE_MESSAGES", "Tracker", ctx.gson().toJson(msgs)));
            System.out.println("[*] Đã giao " + msgs.size() + " tin nhắn vắng mặt cho: " + msg.getSenderId());
        } else {
            ctx.reply(new Message("NO_OFFLINE", "Tracker", ""));
        }
    }
}
