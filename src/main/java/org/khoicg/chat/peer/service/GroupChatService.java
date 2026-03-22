package org.khoicg.chat.peer.service;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.session.PeerSessionContext;
import org.khoicg.chat.util.AESUtil;
import org.khoicg.chat.util.MessageIdUtil;

import java.util.List;
import java.util.Scanner;

public final class GroupChatService {

    private final PeerSessionContext session;
    private final PeerDirectoryService directory;
    private final OfflineMessageService offline;

    public GroupChatService(PeerSessionContext session, PeerDirectoryService directory, OfflineMessageService offline) {
        this.session = session;
        this.directory = directory;
        this.offline = offline;
    }

    public void runInteractive(Scanner scanner) {
        List<PeerInfo> peers = directory.fetchOnlinePeers();
        if (peers == null) {
            System.out.println("[-] Không lấy được danh bạ từ Tracker.");
            return;
        }

        System.out.print("Nhập ID những người muốn chat (cách nhau bằng dấu phẩy), hoặc gõ 'ALL' để gửi toàn mạng: ");
        String targets = scanner.nextLine();
        System.out.print("Nhập nội dung tin nhắn nhóm: ");
        String content = scanner.nextLine();

        String encryptedContent = AESUtil.encrypt(content);
        System.out.println("Đang gửi đi chuỗi mã hóa: " + encryptedContent);
        String batchMsgId = MessageIdUtil.newId();
        Message groupMsg = new Message("GROUP_CHAT", session.myId(), encryptedContent);
        groupMsg.setMessageId(batchMsgId);

        boolean isBroadcast = targets.equalsIgnoreCase("ALL");
        String[] targetIds = targets.split(",");

        int successCount = 0;
        System.out.println("--- ĐANG GỬI TIN NHÓM ---");

        for (PeerInfo p : peers) {
            if (p.getPeerId().equals(session.myId())) {
                continue;
            }

            boolean shouldSend = isBroadcast;
            if (!isBroadcast) {
                for (String t : targetIds) {
                    if (p.getPeerId().equalsIgnoreCase(t.trim())) {
                        shouldSend = true;
                        break;
                    }
                }
            }

            if (shouldSend) {
                System.out.print("Gửi tới " + p.getPeerId() + "... ");
                String ack = session.messaging().sendReliable(p.getIpAddress(), p.getPort(), groupMsg);
                if (ack != null) {
                    System.out.println("[Thành công]");
                    successCount++;
                } else {
                    System.out.println("[Thất bại]");
                    if (encryptedContent != null && offline.tryStore(p.getPeerId(), encryptedContent, batchMsgId)) {
                        System.out.println("    -> Tracker giữ hộ tin cho " + p.getPeerId());
                    }
                }
            }
        }
        System.out.println("-> [Hoàn tất! Đã gửi thành công tới " + successCount + " peer]");
    }
}
