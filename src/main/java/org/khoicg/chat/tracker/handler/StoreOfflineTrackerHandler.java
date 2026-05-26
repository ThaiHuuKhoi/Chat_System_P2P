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
        String[] parts = msg.getContent().split("\\|\\|", 2);
        if (parts.length != 2) {
            return;
        }
        String targetId = parts[0];
        String encryptedContent = parts[1];

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
