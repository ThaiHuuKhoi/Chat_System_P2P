package org.khoicg.chat.peer.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.PeerHandleContext;
import org.khoicg.chat.peer.PeerInboundHandler;

/**
 * Xử lý thông báo PEER_JOINED / PEER_LEFT do Tracker push tới.
 * Chuyển event lên UI listener để cập nhật danh sách peer real-time.
 */
public final class PeerChangedInboundHandler implements PeerInboundHandler {

    @Override
    public boolean supports(String type) {
        return "PEER_JOINED".equals(type) || "PEER_LEFT".equals(type);
    }

    @Override
    public void handle(Message msg, PeerHandleContext ctx) {
        if (ctx.listener() != null) {
            ctx.listener().onEvent(msg.getType(), msg.getSenderId(), msg.getContent());
        }
    }
}
