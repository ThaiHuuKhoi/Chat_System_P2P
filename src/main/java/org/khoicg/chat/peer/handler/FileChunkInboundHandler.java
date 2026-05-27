package org.khoicg.chat.peer.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.PeerHandleContext;
import org.khoicg.chat.peer.PeerInboundHandler;
import org.khoicg.chat.peer.service.FileTransferInbound;
import org.khoicg.chat.peer.service.FileTransferRegistry;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public final class FileChunkInboundHandler implements PeerInboundHandler {

    private final FileTransferRegistry registry;

    public FileChunkInboundHandler(FileTransferRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean supports(String type) {
        return "FILE_CHUNK".equals(type) || "FILE_RESUME_REQ".equals(type);
    }

    @Override
    public void handle(Message msg, PeerHandleContext ctx) {
        if ("FILE_RESUME_REQ".equals(msg.getType())) {
            handleResumeReq(msg, ctx);
        } else {
            handleChunk(msg, ctx);
        }
    }

    private void handleChunk(Message msg, PeerHandleContext ctx) {
        String[] parts = msg.getContent().split("\\|\\|", 5);
        if (parts.length != 5) {
            ctx.writeAck(msg, "NACK:format");
            return;
        }
        String transferId = parts[0];
        // Chỉ lấy tên file, loại bỏ path để chặn path traversal
        String fileName   = Paths.get(parts[1]).getFileName().toString();
        int chunkIdx, totalChunks;
        try {
            chunkIdx    = Integer.parseInt(parts[2]);
            totalChunks = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            ctx.writeAck(msg, "NACK:index");
            return;
        }
        byte[] data = Base64.getDecoder().decode(parts[4]);

        FileTransferInbound transfer = registry.getOrCreateInbound(
                transferId, fileName, totalChunks, ctx.myPort());

        if (ctx.deduper().markFirstDelivery(msg)) {
            try {
                transfer.saveChunk(chunkIdx, data);
                if (transfer.isComplete()) {
                    Path saved = transfer.assemble(ctx.myPort());
                    registry.removeInbound(transferId);
                    System.out.println("\n[!] Nhận file hoàn tất từ " + msg.getSenderId()
                            + ": " + saved.toAbsolutePath());
                    if (ctx.listener() != null) {
                        ctx.listener().onEvent("FILE", msg.getSenderId(),
                                fileName + " → " + saved.toAbsolutePath());
                    }
                }
            } catch (Exception e) {
                System.out.println("[-] Lỗi lưu chunk: " + e.getMessage());
            }
        }
        if (ctx.listener() == null) System.out.print("Chọn chức năng: ");
        ctx.writeAck(msg, "chunk:" + chunkIdx);
    }

    private void handleResumeReq(Message msg, PeerHandleContext ctx) {
        String transferId = msg.getContent() == null ? "" : msg.getContent().trim();
        FileTransferInbound transfer = registry.getInbound(transferId);
        int next = (transfer == null) ? 0 : transfer.nextExpectedChunk();
        ctx.writeAck(msg, String.valueOf(next));
    }
}
