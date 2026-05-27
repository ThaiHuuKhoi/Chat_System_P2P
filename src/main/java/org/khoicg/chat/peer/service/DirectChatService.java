package org.khoicg.chat.peer.service;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.session.PeerSessionContext;
import org.khoicg.chat.util.AESUtil;
import org.khoicg.chat.util.MessageIdUtil;

import java.util.Scanner;

public final class DirectChatService {

    private final PeerSessionContext session;
    private final PeerDirectoryService directory;
    private final OfflineMessageService offline;

    public DirectChatService(PeerSessionContext session, PeerDirectoryService directory, OfflineMessageService offline) {
        this.session = session;
        this.directory = directory;
        this.offline = offline;
    }

    /**
     * Gửi tin nhắn trực tiếp không qua Scanner (dùng cho GUI).
     * @return "OK", "OFFLINE_STORED", hoặc "FAIL"
     */
    public String send(String targetIp, int targetPort, String content) {
        String encrypted = AESUtil.encrypt(content);
        String msgId = MessageIdUtil.newId();
        Message chatMsg = new Message("CHAT", session.myId(), encrypted);
        chatMsg.setMessageId(msgId);
        String ack = session.messaging().sendReliable(targetIp, targetPort, chatMsg);
        if (ack != null) return "OK";
        if (offline.tryStore(targetIp, targetPort, encrypted, msgId)) {
            return "OFFLINE_STORED";
        }
        return "FAIL";
    }

    public void runInteractive(Scanner scanner) {
        System.out.print("Nhập IP người nhận: ");
        String targetIp = scanner.nextLine();
        System.out.print("Nhập Port người nhận: ");
        int targetPort;
        try {
            targetPort = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[-] Port không hợp lệ.");
            return;
        }
        System.out.print("Nhập nội dung tin nhắn: ");
        String content = scanner.nextLine();

        String encryptedContent = AESUtil.encrypt(content);
        System.out.println("Encrypted: " + encryptedContent);
        String msgId = MessageIdUtil.newId();
        Message chatMsg = new Message("CHAT", session.myId(), encryptedContent);
        chatMsg.setMessageId(msgId);

        String ack = session.messaging().sendReliable(targetIp, targetPort, chatMsg);

        if (ack != null) {
            System.out.println("-> [Đã gửi và người nhận ĐÃ XÁC NHẬN (msgId=" + msgId + ")]");
        } else {
            System.out.println("-> [GỬI THẤT BẠI: Tin nhắn không tới được đích]");
            if (encryptedContent != null) {
                if (offline.tryStore(targetIp, targetPort, encryptedContent, msgId)) {
                    System.out.println("-> [Tracker đã lưu tin; peer sẽ nhận khi online lại]");
                } else {
                    System.out.println("-> [Không lưu được tin lên tracker — peer chưa từng đăng ký]");
                }
            }
        }
    }
}
