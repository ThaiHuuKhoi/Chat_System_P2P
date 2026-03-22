package org.khoicg.chat.net;

import org.khoicg.chat.model.Message;

/** ISP: callers that only talk to the tracker depend on this narrow contract. */
public interface TrackerEndpoint {

    String sendTracker(Message message);
}
