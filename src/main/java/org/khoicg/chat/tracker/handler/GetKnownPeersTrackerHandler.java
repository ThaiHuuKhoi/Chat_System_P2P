package org.khoicg.chat.tracker.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.tracker.TrackerHandleContext;
import org.khoicg.chat.tracker.TrackerMessageHandler;

public final class GetKnownPeersTrackerHandler implements TrackerMessageHandler {

    @Override
    public boolean supports(String type) {
        return "GET_KNOWN_PEERS".equals(type);
    }

    @Override
    public void handle(Message msg, TrackerHandleContext ctx) {
        String peerListJson = ctx.gson().toJson(ctx.state().knownPeers());
        ctx.reply(new Message("KNOWN_PEER_LIST", "Tracker", peerListJson));
    }
}
