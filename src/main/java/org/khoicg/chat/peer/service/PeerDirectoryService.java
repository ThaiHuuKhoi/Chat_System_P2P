package org.khoicg.chat.peer.service;

import com.google.gson.reflect.TypeToken;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.lang.reflect.Type;
import java.util.List;

public final class PeerDirectoryService {

    private static final long CACHE_TTL_MS = 5_000;

    private final PeerSessionContext session;
    private List<PeerInfo> cachedPeers;
    private long cacheTimestamp = 0;

    public PeerDirectoryService(PeerSessionContext session) {
        this.session = session;
    }

    /**
     * Trả danh sách peer online. Kết quả được cache 5 giây để tránh mở TCP
     * connection mới vào Tracker trước mỗi thao tác.
     * Khi Tracker không phản hồi, trả lại bản cache cũ (stale-on-error).
     */
    public synchronized List<PeerInfo> fetchOnlinePeers() {
        long now = System.currentTimeMillis();
        if (cachedPeers != null && now - cacheTimestamp < CACHE_TTL_MS) {
            return cachedPeers;
        }
        String jsonResponse = session.messaging().sendTracker(
                new Message("GET_PEERS", session.myId(), ""));
        if (jsonResponse == null) return cachedPeers; // stale-on-error
        try {
            Message respMsg = session.gson().fromJson(jsonResponse, Message.class);
            Type listType = new TypeToken<List<PeerInfo>>() {}.getType();
            List<PeerInfo> fresh = session.gson().fromJson(respMsg.getContent(), listType);
            if (fresh != null) {
                cachedPeers      = fresh;
                cacheTimestamp   = System.currentTimeMillis();
            }
        } catch (Exception ignored) {}
        return cachedPeers;
    }

    /** Buộc lần gọi tiếp theo lấy dữ liệu mới từ Tracker (dùng cho nút Refresh GUI). */
    public synchronized void invalidateCache() {
        cacheTimestamp = 0;
    }
}
