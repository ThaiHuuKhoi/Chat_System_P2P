package org.khoicg.chat.peer.service;

import com.google.gson.reflect.TypeToken;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.session.PeerSessionContext;
import org.khoicg.chat.util.AESUtil;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * STORE_OFFLINE / PULL_OFFLINE flows (SRP).
 */
public final class OfflineMessageService {

    private final PeerSessionContext session;

    public OfflineMessageService(PeerSessionContext session) {
        this.session = session;
    }

    public boolean tryStore(String targetPeerId, String encryptedContent, String messageId) {
        String payload = targetPeerId + "||" + encryptedContent;
        Message store = new Message("STORE_OFFLINE", session.myId(), payload);
        if (messageId != null && !messageId.isEmpty()) {
            store.setMessageId(messageId);
        }
        String resp = session.messaging().sendTracker(store);
        if (resp == null) {
            return false;
        }
        try {
            Message m = session.gson().fromJson(resp, Message.class);
            return "ACK".equals(m.getType());
        } catch (Exception e) {
            return false;
        }
    }

    /** In ra tin offline sau khi đăng nhập (PULL_OFFLINE). */
    public void printPulledOfflineInbox() {
        Message pullMsg = new Message("PULL_OFFLINE", session.myId(), "");
        String pullResponse = session.messaging().sendTracker(pullMsg);
        if (pullResponse == null) {
            return;
        }
        Message respMsg = session.gson().fromJson(pullResponse, Message.class);
        if (!"OFFLINE_MESSAGES".equals(respMsg.getType())) {
            return;
        }
        Type msgListType = new TypeToken<List<Message>>() {}.getType();
        List<Message> missedMsgs = session.gson().fromJson(respMsg.getContent(), msgListType);
        if (missedMsgs == null) {
            missedMsgs = List.of();
        }

        System.out.println("\n🔔 BẠN CÓ " + missedMsgs.size() + " TIN NHẮN KHI ĐANG OFFLINE:");
        Set<String> seenPullKeys = new HashSet<>();
        for (Message m : missedMsgs) {
            String mid = m.getMessageId();
            if (mid != null && !mid.isEmpty()) {
                String key = m.getSenderId() + "|" + mid;
                if (!seenPullKeys.add(key)) {
                    continue;
                }
            }
            String dec = AESUtil.decrypt(m.getContent());
            System.out.println("   -> [Gửi từ " + m.getSenderId() + "]: " + dec);
        }
        System.out.println("----------------------------------------");
    }
}
