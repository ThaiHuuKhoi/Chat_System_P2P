package org.khoicg.chat.peer.service;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.session.PeerSessionContext;
import org.khoicg.chat.util.AESUtil;
import org.khoicg.chat.util.MessageIdUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class GroupChatService {

    private final PeerSessionContext session;
    private final PeerDirectoryService directory;
    private final OfflineMessageService offline;

    public GroupChatService(PeerSessionContext session, PeerDirectoryService directory, OfflineMessageService offline) {
        this.session   = session;
        this.directory = directory;
        this.offline   = offline;
    }

    public record GroupSendResult(int successCount, int storedCount, int failCount) {}

    /** Gửi nhóm không qua Scanner (GUI). */
    public GroupSendResult send(String targetsInput, String content) {
        List<PeerInfo> peers = directory.fetchOnlinePeers();
        if (peers == null) return new GroupSendResult(0, 0, 0);
        String encrypted = AESUtil.encrypt(content);
        String batchMsgId = MessageIdUtil.newId();
        return doSend(buildTargetList(peers, targetsInput), encrypted, batchMsgId);
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
        String content  = scanner.nextLine();

        String encrypted  = AESUtil.encrypt(content);
        String batchMsgId = MessageIdUtil.newId();

        List<PeerInfo> targetList = buildTargetList(peers, targets);
        System.out.println("--- ĐANG GỬI TIN NHÓM (" + targetList.size() + " peer, song song) ---");
        GroupSendResult r = doSend(targetList, encrypted, batchMsgId);
        System.out.println("-> [Hoàn tất! " + r.successCount() + " thành công"
                + (r.storedCount() > 0 ? ", " + r.storedCount() + " lưu offline" : "")
                + (r.failCount()   > 0 ? ", " + r.failCount()   + " thất bại"    : "") + "]");
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private List<PeerInfo> buildTargetList(List<PeerInfo> peers, String targetsInput) {
        boolean isBroadcast = targetsInput.equalsIgnoreCase("ALL");
        String[] targetIds  = targetsInput.split(",");
        return peers.stream()
                .filter(p -> !p.getPeerId().equals(session.myId()))
                .filter(p -> isBroadcast || Arrays.stream(targetIds)
                        .anyMatch(t -> p.getPeerId().equalsIgnoreCase(t.trim())))
                .collect(Collectors.toList());
    }

    /**
     * Gửi tới tất cả peer trong danh sách song song (CompletableFuture).
     * Mỗi peer chạy trên ForkJoinPool.commonPool() — thay bằng dedicated pool
     * nếu số peer > 50 trong môi trường production.
     */
    private GroupSendResult doSend(List<PeerInfo> targets, String encrypted, String batchMsgId) {
        if (encrypted == null) return new GroupSendResult(0, 0, targets.size());

        Message groupMsg = new Message("GROUP_CHAT", session.myId(), encrypted);
        groupMsg.setMessageId(batchMsgId);

        List<CompletableFuture<String>> futures = targets.stream()
                .map(p -> CompletableFuture.supplyAsync(() -> {
                    String ack = session.messaging().sendReliable(p.getIpAddress(), p.getPort(), groupMsg);
                    if (ack != null) return "OK";
                    if (offline.tryStore(p.getIpAddress(), p.getPort(), encrypted, batchMsgId)) return "STORED";
                    return "FAIL";
                }))
                .collect(Collectors.toList());

        int success = 0, stored = 0, fail = 0;
        for (CompletableFuture<String> f : futures) {
            try {
                switch (f.get()) {
                    case "OK"     -> success++;
                    case "STORED" -> stored++;
                    default       -> fail++;
                }
            } catch (Exception e) {
                fail++;
            }
        }
        return new GroupSendResult(success, stored, fail);
    }
}
