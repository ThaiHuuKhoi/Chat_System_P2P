package org.khoicg.chat.peer.service;

import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Đăng ký mạng + heartbeat + inbox offline (SRP).
 */
public final class PeerJoinService {

    private final PeerSessionContext session;
    private final OfflineMessageService offline;

    public PeerJoinService(PeerSessionContext session, OfflineMessageService offline) {
        this.session = session;
        this.offline = offline;
    }

    /**
     * @param joinMode "1" tracker trực tiếp, "2" relay
     */
    public boolean completeJoin(Message registerMessage, String joinMode, String knownIp, int knownPort,
                                AtomicBoolean running) {
        String regResponseJson;
        if ("2".equals(joinMode)) {
            System.out.println("\n[*] Đang nhờ peer " + knownIp + ":" + knownPort + " đăng ký hộ với Tracker...");
            Message relayReg = new Message("RELAY_REGISTER", session.myId(), registerMessage.getContent());
            regResponseJson = session.messaging().sendPeer(knownIp, knownPort, relayReg);
        } else {
            System.out.println("\n[*] Đang kết nối tới Tracker Server (" + AppConfig.trackerHost() + ":" + AppConfig.trackerPort() + ")...");
            regResponseJson = session.messaging().sendTracker(registerMessage);
        }

        Message regParsed = regResponseJson != null ? session.gson().fromJson(regResponseJson, Message.class) : null;
        boolean registerOk = regParsed != null && "REGISTER_OK".equals(regParsed.getType());

        if (registerOk) {
            System.out.println("[+] " + regParsed.getContent());
            HeartbeatService.startDaemon(session, running);
            offline.printPulledOfflineInbox();
            return true;
        }

        if (regParsed != null && "REGISTER_FAIL".equals(regParsed.getType())) {
            System.out.println("[-] Peer relay không đăng ký được với Tracker: " + regParsed.getContent());
        } else if ("2".equals(joinMode)) {
            System.out.println("[-] Không gửi được RELAY_REGISTER tới peer đã biết (peer tắt / sai IP:port).");
        } else {
            System.out.println("[-] Không thể kết nối tới Tracker. Vui lòng bật TrackerServer trước!");
        }
        return false;
    }
}
