package org.khoicg.chat.peer.session;

import com.google.gson.Gson;
import org.khoicg.chat.net.MessagingClient;

/**
 * Immutable session data for one peer CLI (DIP: handlers depend on this, not global statics).
 */
public final class PeerSessionContext {

    private final String myId;
    private final int myPort;
    private final MessagingClient messaging;
    private final Gson gson;

    public PeerSessionContext(String myId, int myPort, MessagingClient messaging, Gson gson) {
        this.myId = myId;
        this.myPort = myPort;
        this.messaging = messaging;
        this.gson = gson;
    }

    public String myId() {
        return myId;
    }

    public int myPort() {
        return myPort;
    }

    public MessagingClient messaging() {
        return messaging;
    }

    public Gson gson() {
        return gson;
    }
}
