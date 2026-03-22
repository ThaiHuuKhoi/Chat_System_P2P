package org.khoicg.chat.peer.service;

import com.google.gson.reflect.TypeToken;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Tracker-backed peer listing (SRP).
 */
public final class PeerDirectoryService {

    private final PeerSessionContext session;

    public PeerDirectoryService(PeerSessionContext session) {
        this.session = session;
    }

    public List<PeerInfo> fetchOnlinePeers() {
        String jsonResponse = session.messaging().sendTracker(new Message("GET_PEERS", session.myId(), ""));
        if (jsonResponse == null) {
            return null;
        }
        try {
            Message respMsg = session.gson().fromJson(jsonResponse, Message.class);
            Type listType = new TypeToken<List<PeerInfo>>() {}.getType();
            return session.gson().fromJson(respMsg.getContent(), listType);
        } catch (Exception e) {
            return null;
        }
    }

}
