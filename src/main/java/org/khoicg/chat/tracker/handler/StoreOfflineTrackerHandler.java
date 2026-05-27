package org.khoicg.chat.tracker.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.tracker.TrackerHandleContext;
import org.khoicg.chat.tracker.TrackerMessageHandler;

public final class StoreOfflineTrackerHandler implements TrackerMessageHandler {

    @Override
    public boolean supports(String type) {
        return "STORE_OFFLINE".equals(type);
    }

    @Override
    public void handle(Message msg, TrackerHandleContext ctx) {
        String[] parts = msg.getContent().split("\\|\\|", 3);
        String targetId;
        String encryptedContent;

        if (parts.length == 2) {
            // Format từ relay: peerId||encrypted
            targetId = parts[0].trim();
            encryptedContent = parts[1];
            PeerInfo known = ctx.state().findKnownPeerById(targetId);
            if (known == null) {
                System.out.println("[!] STORE_OFFLINE: không tìm thấy peer ID=" + targetId);
                ctx.reply(new Message("NACK", "Tracker", "Không tìm thấy peer theo ID"));
                return;
            }
        } else if (parts.length == 3) {
            // Format từ DirectChat: ip||port||encrypted
            String targetIp      = parts[0];
            String targetPortStr = parts[1];
            encryptedContent     = parts[2];
            int targetPort;
            try {
                targetPort = Integer.parseInt(targetPortStr);
            } catch (NumberFormatException e) {
                ctx.reply(new Message("NACK", "Tracker", "Port không hợp lệ"));
                return;
            }
            targetId = ctx.state().findKnownPeerByAddress(targetIp, targetPort);
            if (targetId == null) {
                System.out.println("[!] STORE_OFFLINE: không tìm thấy peer tại " + targetIp + ":" + targetPortStr);
                ctx.reply(new Message("NACK", "Tracker", "Không tìm thấy peer theo địa chỉ"));
                return;
            }
        } else {
            ctx.reply(new Message("NACK", "Tracker", "Định dạng STORE_OFFLINE không hợp lệ"));
            return;
        }

        Message storedMsg = new Message("OFFLINE_CHAT", msg.getSenderId(), encryptedContent);
        if (msg.getMessageId() != null && !msg.getMessageId().isEmpty()) {
            storedMsg.setMessageId(msg.getMessageId());
        }

        boolean stored = ctx.state().storeOfflineFor(targetId, storedMsg);
        if (stored) {
            System.out.println("[*] Nhận giữ hộ 1 tin nhắn cho: " + targetId);
            ctx.reply(new Message("ACK", "Tracker", "Đã lưu"));
        } else {
            System.out.println("[!] Hàng đợi offline của " + targetId + " đã đầy — từ chối lưu");
            ctx.reply(new Message("NACK", "Tracker", "Hàng đợi offline đã đầy"));
        }
    }
}
