package org.khoicg.chat.tracker.handler;

import org.khoicg.chat.model.Message;
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
        if (parts.length != 3) {
            ctx.reply(new Message("NACK", "Tracker", "Định dạng STORE_OFFLINE không hợp lệ"));
            return;
        }
        String targetIp       = parts[0];
        String targetPortStr  = parts[1];
        String encryptedContent = parts[2];

        int targetPort;
        try {
            targetPort = Integer.parseInt(targetPortStr);
        } catch (NumberFormatException e) {
            ctx.reply(new Message("NACK", "Tracker", "Port không hợp lệ"));
            return;
        }

        String targetId = ctx.state().findKnownPeerByAddress(targetIp, targetPort);
        if (targetId == null) {
            System.out.println("[!] STORE_OFFLINE: không tìm thấy peer tại " + targetIp + ":" + targetPort);
            ctx.reply(new Message("NACK", "Tracker", "Không tìm thấy peer theo địa chỉ"));
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
