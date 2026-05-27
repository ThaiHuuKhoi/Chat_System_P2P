package org.khoicg.chat.peer.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.TreeSet;

public final class FileTransferInbound {

    private final String transferId;
    private final String fileName;
    private final int totalChunks;
    private final TreeSet<Integer> receivedChunks = new TreeSet<>();
    private final Path tempDir;
    private volatile long lastActivity;

    public FileTransferInbound(String transferId, String fileName, int totalChunks, int myPort)
            throws IOException {
        this.transferId = transferId;
        this.fileName = fileName;
        this.totalChunks = totalChunks;
        this.lastActivity = System.currentTimeMillis();
        this.tempDir = Files.createDirectories(
                Paths.get("downloads_" + myPort, "tmp", transferId));
    }

    public synchronized void saveChunk(int idx, byte[] data) throws IOException {
        Files.write(tempDir.resolve(String.format("%08d", idx)), data);
        receivedChunks.add(idx);
        lastActivity = System.currentTimeMillis();
    }

    public synchronized boolean isComplete() {
        return receivedChunks.size() == totalChunks;
    }

    /** Chunk nhỏ nhất chưa nhận được — dùng để báo cho sender biết resume từ đâu. */
    public synchronized int nextExpectedChunk() {
        for (int i = 0; i < totalChunks; i++) {
            if (!receivedChunks.contains(i)) return i;
        }
        return totalChunks;
    }

    /** Ghép tất cả chunk thành file hoàn chỉnh, xóa thư mục tạm. */
    public Path assemble(int myPort) throws IOException {
        Path outDir = Files.createDirectories(Paths.get("downloads_" + myPort));
        Path out = outDir.resolve(fileName);
        try (OutputStream os = Files.newOutputStream(out)) {
            for (int i = 0; i < totalChunks; i++) {
                os.write(Files.readAllBytes(tempDir.resolve(String.format("%08d", i))));
            }
        }
        try (var stream = Files.walk(tempDir)) {
            stream.sorted(Comparator.reverseOrder())
                  .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
        return out;
    }

    public String getTransferId() { return transferId; }
    public String getFileName()   { return fileName; }
    public long   getLastActivity() { return lastActivity; }
}
