package org.khoicg.chat.net;

import org.khoicg.chat.model.Message;

/** ISP: callers that only contact other peers depend on this narrow contract. */
public interface PeerEndpoint {

    String sendReliable(String host, int port, Message message);

    String sendPeer(String host, int port, Message message);
}
