package org.khoicg.chat.peer.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.PeerHandleContext;
import org.khoicg.chat.peer.PeerInboundHandler;
import org.khoicg.chat.util.AESUtil;

public final class ChatInboundHandler implements PeerInboundHandler {

    @Override
    public boolean supports(String type) {
        return "CHAT".equals(type) || "GROUP_CHAT".equals(type);
    }

    @Override
    public void handle(Message msg, PeerHandleContext ctx) {
        boolean firstTime = ctx.deduper().markFirstDelivery(msg);
        if (firstTime) {
            String decryptedContent = AESUtil.decrypt(msg.getContent());
            String prefix = "GROUP_CHAT".equals(msg.getType()) ? "[Tin nhắn Nhóm từ " : "[Tin nhắn từ ";
            System.out.println("\n" + prefix + msg.getSenderId() + "]: " + decryptedContent);
        } else {
            System.out.println("\n[Bỏ qua tin trùng — cùng mã tin từ " + msg.getSenderId() + "]");
        }
        System.out.print("Chọn chức năng: ");
        ctx.writeAck(msg, "Đã nhận");
    }
}
