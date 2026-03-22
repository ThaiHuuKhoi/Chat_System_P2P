package org.khoicg.chat.peer.service;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.session.PeerSessionContext;
import org.khoicg.chat.util.AESUtil;
import org.khoicg.chat.util.MessageIdUtil;

import java.util.List;
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

    public void runInteractive(Scanner scanner) {
        System.out.print("Nhập IP người nhận: ");
        String targetIp = scanner.nextLine();
        System.out.print("Nhập Port người nhận: ");
        int targetPort = Integer.parseInt(scanner.nextLine());
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
                List<PeerInfo> peers = directory.fetchOnlinePeers();
                String targetPeerId = PeerAddressUtil.resolvePeerIdByAddress(peers, targetIp, targetPort);
                if (targetPeerId != null) {
                    if (offline.tryStore(targetPeerId, encryptedContent, msgId)) {
                        System.out.println("-> [Tracker đã lưu tin; " + targetPeerId + " sẽ nhận khi online lại]");
                    } else {
                        System.out.println("-> [Không lưu được tin lên tracker]");
                    }
                } else {
                    System.out.println("-> [Không lưu offline: IP/Port không khớp peer nào trên tracker — xem menu 1]");
                }
            }
        }
    }
}
