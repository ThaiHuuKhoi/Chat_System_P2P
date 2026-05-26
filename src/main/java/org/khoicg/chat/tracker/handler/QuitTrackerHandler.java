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
        String quitId = msg.getSenderId();
        ctx.state().removePeer(quitId);
        System.out.println("[-] Peer chủ động thoát: " + quitId);
        ctx.reply(new Message("QUIT_OK", "Tracker", "Đã xóa khỏi hệ thống"));

        // Push PEER_LEFT tới tất cả peer còn lại
        Message leftMsg = new Message("PEER_LEFT", "Tracker", quitId);
        ctx.broadcaster().broadcast(leftMsg, null, ctx.state().onlinePeers());
    }
}
