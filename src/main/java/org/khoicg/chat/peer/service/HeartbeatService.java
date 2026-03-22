package org.khoicg.chat.peer.service;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.concurrent.atomic.AtomicBoolean;

public final class HeartbeatService {

    private HeartbeatService() {}

    public static void startDaemon(PeerSessionContext session, AtomicBoolean running) {
        Thread heartbeatThread = new Thread(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(5000);
                    Message pingMsg = new Message("HEARTBEAT", session.myId(), "ping");
                    session.messaging().sendTracker(pingMsg);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {
                    // tracker down
                }
            }
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }
}
