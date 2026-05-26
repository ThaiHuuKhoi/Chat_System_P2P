package org.khoicg.chat.peer.service;

import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class HeartbeatService {

    private static final int INTERVAL_MS  = 5_000;
    private static final int MAX_FAILURES = 3;

    private HeartbeatService() {}

    public static void startDaemon(PeerSessionContext session, AtomicBoolean running) {
        Thread t = new Thread(() -> {
            AtomicInteger failCount = new AtomicInteger(0);
            while (running.get()) {
                try {
                    Thread.sleep(INTERVAL_MS);
                    Message ping = new Message("HEARTBEAT", session.myId(), "ping");
                    String resp = session.messaging().sendTracker(ping);
                    if (resp != null) {
                        failCount.set(0);
                    } else if (failCount.incrementAndGet() >= MAX_FAILURES) {
                        reregister(session);
                        failCount.set(0);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    if (failCount.incrementAndGet() >= MAX_FAILURES) {
                        reregister(session);
                        failCount.set(0);
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static void reregister(PeerSessionContext session) {
        try {
            System.out.println("[!] Tracker không phản hồi " + MAX_FAILURES
                    + " lần liên tiếp — đang đăng ký lại...");
            PeerInfo self = new PeerInfo(session.myId(), AppConfig.peerAdvertiseHost(), session.myPort());
            Message regMsg = new Message("REGISTER", session.myId(), session.gson().toJson(self));
            String resp = session.messaging().sendTracker(regMsg);
            if (resp != null) {
                System.out.println("[+] Kết nối lại Tracker thành công.");
            } else {
                System.out.println("[-] Chưa kết nối lại được — sẽ thử sau.");
            }
        } catch (Exception ignored) {}
    }
}
