package org.khoicg.chat.peer;

import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.net.MessagingClient;

/**
 * Default {@link MessagingClient}: tracker endpoint from {@link AppConfig}, transport via {@link PeerClient}.
 */
public final class PeerMessagingAdapter implements MessagingClient {

    private final PeerClient peerClient;
    private final String trackerHost;
    private final int trackerPort;

    public PeerMessagingAdapter(PeerClient peerClient) {
        this(peerClient, AppConfig.trackerHost(), AppConfig.trackerPort());
    }

    public PeerMessagingAdapter(PeerClient peerClient, String trackerHost, int trackerPort) {
        this.peerClient = peerClient;
        this.trackerHost = trackerHost;
        this.trackerPort = trackerPort;
    }

    @Override
    public String sendTracker(Message message) {
        return peerClient.sendRequest(trackerHost, trackerPort, message);
    }

    @Override
    public String sendReliable(String host, int port, Message message) {
        return peerClient.sendReliableRequest(host, port, message);
    }

    @Override
    public String sendPeer(String host, int port, Message message) {
        return peerClient.sendRequest(host, port, message);
    }
}
