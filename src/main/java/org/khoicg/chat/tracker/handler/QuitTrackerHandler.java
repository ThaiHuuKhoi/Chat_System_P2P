package org.khoicg.chat.tracker.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.tracker.TrackerHandleContext;
import org.khoicg.chat.tracker.TrackerMessageHandler;

public final class QuitTrackerHandler implements TrackerMessageHandler {

    @Override
    public boolean supports(String type) {
        return "QUIT".equals(type);
    }

    @Override
    public void handle(Message msg, TrackerHandleContext ctx) {
        ctx.state().removePeer(msg.getSenderId());
        System.out.println("[-] Peer chủ động thoát: " + msg.getSenderId());
        ctx.reply(new Message("QUIT_OK", "Tracker", "Đã xóa khỏi hệ thống"));
    }
}
