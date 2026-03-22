package org.khoicg.chat.tracker.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.tracker.TrackerHandleContext;
import org.khoicg.chat.tracker.TrackerMessageHandler;

public final class RegisterTrackerHandler implements TrackerMessageHandler {

    @Override
    public boolean supports(String type) {
        return "REGISTER".equals(type);
    }

    @Override
    public void handle(Message msg, TrackerHandleContext ctx) {
        PeerInfo newPeer = ctx.gson().fromJson(msg.getContent(), PeerInfo.class);
        ctx.state().registerPeer(newPeer);
        System.out.println("[+] Mới gia nhập: " + newPeer);
        ctx.reply(new Message("REGISTER_OK", "Tracker", "Thành công!"));
    }
}
