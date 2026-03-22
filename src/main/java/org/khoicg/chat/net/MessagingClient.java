package org.khoicg.chat.net;

import org.khoicg.chat.model.Message;

/**
 * Full peer networking facade: {@link TrackerEndpoint} + {@link PeerEndpoint} (ISP + DIP).
 * Implementations wrap {@link org.khoicg.chat.peer.PeerClient} and configuration.
 */
public interface MessagingClient extends TrackerEndpoint, PeerEndpoint {
}
