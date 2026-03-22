package org.khoicg.chat.peer.handler;

import com.google.gson.reflect.TypeToken;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.PeerHandleContext;
import org.khoicg.chat.peer.PeerInboundHandler;
import org.khoicg.chat.util.AESUtil;
import org.khoicg.chat.util.ReliableDeliveryHelper;

import java.lang.reflect.Type;
import java.util.List;

public final class RelayInboundHandler implements PeerInboundHandler {

    @Override
    public boolean supports(String type) {
        return "RELAY".equals(type);
    }

    @Override
    public void handle(Message msg, PeerHandleContext ctx) {
        String raw = msg.getContent();
        if (raw == null) {
            ctx.out().println(ctx.gson().toJson(
                    new Message("RELAY_FAIL", "PeerServer", "Nội dung rỗng")));
            return;
        }
        String[] parts = raw.split("\\|\\|", 2);
        if (parts.length < 2) {
            ctx.out().println(ctx.gson().toJson(
                    new Message("RELAY_FAIL", "PeerServer", "Sai định dạng RELAY")));
            return;
        }
        String targetId = parts[0].trim();
        String encrypted = parts[1];

        if (targetId.equals(ctx.myPeerId())) {
            String decrypted = AESUtil.decrypt(encrypted);
            System.out.println("\n[Tin relay từ " + msg.getSenderId() + "]: " + decrypted);
            System.out.print("Chọn chức năng: ");
            ctx.writeAckFromPeer(msg, "Đã nhận");
            return;
        }

        String listJson = ctx.messaging().sendTracker(new Message("GET_PEERS", ctx.myPeerId(), ""));
        if (listJson == null) {
            ctx.out().println(ctx.gson().toJson(
                    new Message("RELAY_FAIL", "PeerServer", "Không tới tracker")));
            return;
        }
        try {
            Message respMsg = ctx.gson().fromJson(listJson, Message.class);
            Type listType = new TypeToken<List<PeerInfo>>() {}.getType();
            List<PeerInfo> peers = ctx.gson().fromJson(respMsg.getContent(), listType);
            PeerInfo dest = null;
            if (peers != null) {
                for (PeerInfo p : peers) {
                    if (p.getPeerId().equals(targetId)) {
                        dest = p;
                        break;
                    }
                }
            }
            if (dest == null) {
                ctx.out().println(ctx.gson().toJson(
                        new Message("RELAY_FAIL", "PeerServer", "Không tìm thấy đích trên tracker")));
                return;
            }

            System.out.println("[Relay] Chuyển tiếp tin của " + msg.getSenderId() + " → " + targetId);
            Message chat = new Message("CHAT", msg.getSenderId(), encrypted);
            chat.setMessageId(msg.getMessageId());
            String ack = ctx.messaging().sendReliable(dest.getIpAddress(), dest.getPort(), chat);
            if (ReliableDeliveryHelper.ackJsonMatches(ack, chat.getMessageId())) {
                ctx.writeAckFromPeer(msg, "Đã chuyển tiếp");
                return;
            }

            Message store = new Message("STORE_OFFLINE", msg.getSenderId(), targetId + "||" + encrypted);
            store.setMessageId(msg.getMessageId());
            String sresp = ctx.messaging().sendTracker(store);
            boolean stored = false;
            if (sresp != null) {
                try {
                    Message sm = ctx.gson().fromJson(sresp, Message.class);
                    stored = sm != null && "ACK".equals(sm.getType());
                } catch (Exception ignored) {
                }
            }
            if (stored) {
                ctx.writeAckFromPeer(msg, "Đích offline — tracker giữ hộ");
            } else {
                ctx.out().println(ctx.gson().toJson(
                        new Message("RELAY_FAIL", "PeerServer", "Chuyển tiếp thất bại")));
            }
        } catch (Exception e) {
            ctx.out().println(ctx.gson().toJson(
                    new Message("RELAY_FAIL", "PeerServer", e.getMessage())));
        }
    }
}
