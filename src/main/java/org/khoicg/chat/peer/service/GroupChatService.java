package org.khoicg.chat.peer.service;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.session.PeerSessionContext;
import org.khoicg.chat.util.AESUtil;
import org.khoicg.chat.util.MessageIdUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
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
        String encrypted  = AESUtil.encrypt(content);
        String batchMsgId = MessageIdUtil.newId();
        Lists targets = resolveTargets(targetsInput);
        return doSend(targets.online, targets.offline, encrypted, batchMsgId);
    }

    public void runInteractive(Scanner scanner) {
        System.out.print("Nhập ID những người muốn chat (cách nhau bằng dấu phẩy), hoặc gõ 'ALL' để gửi toàn mạng: ");
        String targetsInput = scanner.nextLine();
        System.out.print("Nhập nội dung tin nhắn nhóm: ");
        String content = scanner.nextLine();

        String encrypted  = AESUtil.encrypt(content);
        String batchMsgId = MessageIdUtil.newId();

        Lists targets = resolveTargets(targetsInput);
        int total = targets.online.size() + targets.offline.size();
        System.out.println("--- ĐANG GỬI TIN NHÓM (" + total + " peer, song song) ---");
        GroupSendResult r = doSend(targets.online, targets.offline, encrypted, batchMsgId);
        System.out.println("-> [Hoàn tất! " + r.successCount() + " thành công"
                + (r.storedCount() > 0 ? ", " + r.storedCount() + " lưu offline" : "")
                + (r.failCount()   > 0 ? ", " + r.failCount()   + " thất bại"    : "") + "]");
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private record Lists(List<PeerInfo> online, List<PeerInfo> offline) {}

    private Lists resolveTargets(String targetsInput) {
        List<PeerInfo> onlinePeers = directory.fetchOnlinePeers();
        List<PeerInfo> knownPeers  = directory.fetchKnownPeers();
        if (onlinePeers == null) onlinePeers = List.of();

        Set<String> onlineIds = onlinePeers.stream()
                .map(PeerInfo::getPeerId).collect(Collectors.toSet());

        List<PeerInfo> online  = filter(onlinePeers, targetsInput);
        List<PeerInfo> offline = filter(
                knownPeers.stream()
                        .filter(p -> !onlineIds.contains(p.getPeerId()))
                        .collect(Collectors.toList()),
                targetsInput);
        return new Lists(online, offline);
    }

    private List<PeerInfo> filter(List<PeerInfo> peers, String targetsInput) {
        boolean isBroadcast = targetsInput.equalsIgnoreCase("ALL");
        String[] targetIds  = targetsInput.split(",");
        return peers.stream()
                .filter(p -> !p.getPeerId().equals(session.myId()))
                .filter(p -> isBroadcast || Arrays.stream(targetIds)
                        .anyMatch(t -> p.getPeerId().equalsIgnoreCase(t.trim())))
                .collect(Collectors.toList());
    }

    /**
     * Gửi tới tất cả peer song song (CompletableFuture).
     * Online targets: sendReliable → fallback tryStore.
     * Offline targets: tryStore trực tiếp (không cần thử sendReliable).
     */
    private GroupSendResult doSend(List<PeerInfo> onlineTargets, List<PeerInfo> offlineTargets,
                                   String encrypted, String batchMsgId) {
        if (encrypted == null) {
            int total = onlineTargets.size() + offlineTargets.size();
            return new GroupSendResult(0, 0, total);
        }

        Message groupMsg = new Message("GROUP_CHAT", session.myId(), encrypted);
        groupMsg.setMessageId(batchMsgId);

        List<CompletableFuture<String>> futures = new ArrayList<>();

        onlineTargets.forEach(p -> futures.add(CompletableFuture.supplyAsync(() -> {
            String ack = session.messaging().sendReliable(p.getIpAddress(), p.getPort(), groupMsg);
            if (ack != null) return "OK";
            if (offline.tryStore(p.getIpAddress(), p.getPort(), encrypted, batchMsgId)) return "STORED";
            return "FAIL";
        })));

        offlineTargets.forEach(p -> futures.add(CompletableFuture.supplyAsync(() -> {
            if (offline.tryStore(p.getIpAddress(), p.getPort(), encrypted, batchMsgId)) return "STORED";
            return "FAIL";
        })));

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
