package org.khoicg.chat.peer;

import com.google.gson.Gson;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.net.MessagingClient;

import java.io.PrintWriter;

public final class PeerHandleContext {

    private final String myPeerId;
    private final int myPort;
    private final Gson gson;
    private final PrintWriter out;
    private final MessagingClient messaging;
    private final DeliveryIdempotencyTracker deduper;

    public PeerHandleContext(String myPeerId, int myPort, Gson gson, PrintWriter out,
                             MessagingClient messaging, DeliveryIdempotencyTracker deduper) {
        this.myPeerId = myPeerId;
        this.myPort = myPort;
        this.gson = gson;
        this.out = out;
        this.messaging = messaging;
        this.deduper = deduper;
    }

    public String myPeerId() {
        return myPeerId;
    }

    public int myPort() {
        return myPort;
    }

    public Gson gson() {
        return gson;
    }

    public PrintWriter out() {
        return out;
    }

    public MessagingClient messaging() {
        return messaging;
    }

    public DeliveryIdempotencyTracker deduper() {
        return deduper;
    }

    public void writeAck(Message incoming, String ackText) {
        Message ack = new Message("ACK", "Server", ackText);
        ack.setMessageId(incoming.getMessageId());
        out.println(gson.toJson(ack));
    }

    public void writeAckFromPeer(Message incoming, String ackText) {
        Message ack = new Message("ACK", "PeerServer", ackText);
        ack.setMessageId(incoming.getMessageId());
        out.println(gson.toJson(ack));
    }
}
