package org.khoicg.chat.tracker.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.tracker.TrackerHandleContext;
import org.khoicg.chat.tracker.TrackerMessageHandler;

public final class GetPeersTrackerHandler implements TrackerMessageHandler {

    @Override
    public boolean supports(String type) {
        return "GET_PEERS".equals(type);
    }

    @Override
    public void handle(Message msg, TrackerHandleContext ctx) {
        String peerListJson = ctx.gson().toJson(ctx.state().onlinePeers());
        ctx.reply(new Message("PEER_LIST", "Tracker", peerListJson));
        System.out.println("[*] Đã gửi danh bạ cho: " + msg.getSenderId());
    }
}
