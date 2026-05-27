package org.khoicg.chat.peer.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class FileTransferRegistry {

    private final ConcurrentHashMap<String, FileTransferOutbound> outbound = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FileTransferInbound>  inbound  = new ConcurrentHashMap<>();
    private final long timeoutMs;
    private volatile Consumer<FileTransferOutbound> resumeCallback;

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "file-transfer-cleaner");
        t.setDaemon(true);
        return t;
    });

    public FileTransferRegistry(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        cleaner.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.HOURS);
    }

    public void setResumeCallback(Consumer<FileTransferOutbound> cb) {
        this.resumeCallback = cb;
    }

    // ─── outbound ────────────────────────────────────────────────────────────────

    public void registerOutbound(FileTransferOutbound t) {
        outbound.put(t.getTransferId(), t);
    }

    public void removeOutbound(String transferId) {
        outbound.remove(transferId);
    }

    public List<FileTransferOutbound> findPendingOutboundByTarget(String ip, int port) {
        List<FileTransferOutbound> result = new ArrayList<>();
        for (FileTransferOutbound t : outbound.values()) {
            if (t.targetsAddress(ip, port)) result.add(t);
        }
        return result;
    }

    /** Kích hoạt resume trên background thread — không block handler. */
    public void triggerResume(FileTransferOutbound transfer) {
        Consumer<FileTransferOutbound> cb = resumeCallback;
        if (cb == null) return;
        Thread t = new Thread(() -> cb.accept(transfer), "file-resume-" + transfer.getTransferId());
        t.setDaemon(true);
        t.start();
    }

    // ─── inbound ─────────────────────────────────────────────────────────────────

    public FileTransferInbound getOrCreateInbound(String transferId, String fileName,
                                                   int totalChunks, int myPort) {
        return inbound.computeIfAbsent(transferId, id -> {
            try {
                return new FileTransferInbound(id, fileName, totalChunks, myPort);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public FileTransferInbound getInbound(String transferId) {
        return inbound.get(transferId);
    }

    public void removeInbound(String transferId) {
        inbound.remove(transferId);
    }

    // ─── cleanup ─────────────────────────────────────────────────────────────────

    private void cleanup() {
        long now = System.currentTimeMillis();
        outbound.values().removeIf(t -> now - t.getCreatedAt()    > timeoutMs);
        inbound.values().removeIf( t -> now - t.getLastActivity() > timeoutMs);
    }
}
