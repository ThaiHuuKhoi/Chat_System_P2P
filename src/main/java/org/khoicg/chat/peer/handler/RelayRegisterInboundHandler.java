package org.khoicg.chat.peer.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.PeerHandleContext;
import org.khoicg.chat.peer.PeerInboundHandler;

public final class RelayRegisterInboundHandler implements PeerInboundHandler {

    @Override
    public boolean supports(String type) {
        return "RELAY_REGISTER".equals(type);
    }

    @Override
    public void handle(Message msg, PeerHandleContext ctx) {
        try {
            PeerInfo joining = ctx.gson().fromJson(msg.getContent(), PeerInfo.class);
            System.out.println("\n[Relay đăng ký] Nhận yêu cầu thay mặt peer: " + joining.getPeerId());
            System.out.print("Chọn chức năng: ");
        } catch (Exception ignored) {
        }
        Message toTracker = new Message("REGISTER", msg.getSenderId(), msg.getContent());
        String tr = ctx.messaging().sendTracker(toTracker);
        if (tr != null) {
            ctx.out().println(tr);
        } else {
            ctx.out().println(ctx.gson().toJson(
                    new Message("REGISTER_FAIL", "PeerServer", "Tracker không phản hồi")));
        }
    }
}
