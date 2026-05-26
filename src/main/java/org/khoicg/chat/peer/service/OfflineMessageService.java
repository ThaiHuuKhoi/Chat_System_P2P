package org.khoicg.chat.peer.service;

import com.google.gson.reflect.TypeToken;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.session.PeerSessionContext;
import org.khoicg.chat.util.AESUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        if (resp == null) return false;
        try {
            Message m = session.gson().fromJson(resp, Message.class);
            return "ACK".equals(m.getType());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kéo tin nhắn offline từ Tracker, giải mã, và trả về danh sách chuỗi hiển thị.
     * Dùng cho GUI — không in ra console.
     */
    public List<String> pullOfflineMessages() {
        Message pullMsg = new Message("PULL_OFFLINE", session.myId(), "");
        String pullResponse = session.messaging().sendTracker(pullMsg);
        if (pullResponse == null) return List.of();
        Message respMsg = session.gson().fromJson(pullResponse, Message.class);
        if (!"OFFLINE_MESSAGES".equals(respMsg.getType())) return List.of();

        Type msgListType = new TypeToken<List<Message>>() {}.getType();
        List<Message> missedMsgs = session.gson().fromJson(respMsg.getContent(), msgListType);
        if (missedMsgs == null) return List.of();

        Set<String> seen = new HashSet<>();
        List<String> result = new ArrayList<>();
        for (Message m : missedMsgs) {
            String mid = m.getMessageId();
            if (mid != null && !mid.isEmpty()) {
                if (!seen.add(m.getSenderId() + "|" + mid)) continue;
            }
            result.add("[Gửi từ " + m.getSenderId() + "]: " + AESUtil.decrypt(m.getContent()));
        }
        return result;
    }

    /** In ra inbox offline sau khi đăng nhập (dùng cho console). */
    public void printPulledOfflineInbox() {
        List<String> msgs = pullOfflineMessages();
        if (msgs.isEmpty()) return;
        System.out.println("\n🔔 BẠN CÓ " + msgs.size() + " TIN NHẮN KHI ĐANG OFFLINE:");
        msgs.forEach(m -> System.out.println("   -> " + m));
        System.out.println("----------------------------------------");
    }
}
