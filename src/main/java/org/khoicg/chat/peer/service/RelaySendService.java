package org.khoicg.chat.peer.service;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.session.PeerSessionContext;
import org.khoicg.chat.util.AESUtil;
import org.khoicg.chat.util.MessageIdUtil;

import java.util.Scanner;

public final class RelaySendService {

    private final PeerSessionContext session;

    public RelaySendService(PeerSessionContext session) {
        this.session = session;
    }

    public void runInteractive(Scanner scanner) {
        System.out.print("IP peer trung gian (relay): ");
        String relayIp = scanner.nextLine();
        System.out.print("Port peer trung gian: ");
        int relayPort = Integer.parseInt(scanner.nextLine());
        System.out.print("Peer ID người nhận cuối: ");
        String finalTargetId = scanner.nextLine().trim();
        System.out.print("Nội dung tin nhắn: ");
        String relayText = scanner.nextLine();

        String encRelay = AESUtil.encrypt(relayText);
        if (encRelay == null) {
            System.out.println("[-] Lỗi mã hóa.");
            return;
        }
        Message relayMsg = new Message("RELAY", session.myId(), finalTargetId + "||" + encRelay);
        relayMsg.setMessageId(MessageIdUtil.newId());
        String relayAck = session.messaging().sendReliable(relayIp, relayPort, relayMsg);
        if (relayAck != null) {
            try {
                Message rm = session.gson().fromJson(relayAck, Message.class);
                if ("ACK".equals(rm.getType())) {
                    System.out.println("-> [Relay thành công: " + rm.getContent() + "]");
                } else if ("RELAY_FAIL".equals(rm.getType())) {
                    System.out.println("-> [Relay thất bại: " + rm.getContent() + "]");
                } else {
                    System.out.println("-> [Phản hồi: " + relayAck + "]");
                }
            } catch (Exception e) {
                System.out.println("-> [Phản hồi: " + relayAck + "]");
            }
        } else {
            System.out.println("-> [Không có ACK hợp lệ từ peer trung gian sau các lần thử]");
        }
    }
}
