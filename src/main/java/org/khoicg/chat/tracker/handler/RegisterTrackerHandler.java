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
        PeerInfo newPeer;
        try {
            newPeer = ctx.gson().fromJson(msg.getContent(), PeerInfo.class);
        } catch (Exception e) {
            newPeer = null;
        }
        if (newPeer == null || newPeer.getPeerId() == null) {
            ctx.reply(new Message("REGISTER_FAIL", "Tracker", "Dữ liệu PeerInfo không hợp lệ"));
            return;
        }
        ctx.state().registerPeer(newPeer);
        System.out.println("[+] Mới gia nhập: " + newPeer);
        ctx.reply(new Message("REGISTER_OK", "Tracker", "Thành công!"));

        // Push PEER_JOINED tới tất cả peer đang online (trừ peer vừa join)
        Message joinMsg = new Message("PEER_JOINED", "Tracker", ctx.gson().toJson(newPeer));
        ctx.broadcaster().broadcast(joinMsg, newPeer.getPeerId(), ctx.state().onlinePeers());
    }
}
