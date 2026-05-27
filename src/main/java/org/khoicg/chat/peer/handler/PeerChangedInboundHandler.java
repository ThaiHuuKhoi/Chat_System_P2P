package org.khoicg.chat.peer.handler;

import com.google.gson.JsonSyntaxException;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.PeerHandleContext;
import org.khoicg.chat.peer.PeerInboundHandler;
import org.khoicg.chat.peer.service.FileTransferOutbound;
import org.khoicg.chat.peer.service.FileTransferRegistry;

import java.util.List;

/**
 * Xử lý thông báo PEER_JOINED / PEER_LEFT do Tracker push tới.
 * Khi PEER_JOINED, tự động resume các file transfer còn dở với peer đó.
 */
public final class PeerChangedInboundHandler implements PeerInboundHandler {

    private final FileTransferRegistry registry;

    public PeerChangedInboundHandler(FileTransferRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean supports(String type) {
        return "PEER_JOINED".equals(type) || "PEER_LEFT".equals(type);
    }

    @Override
    public void handle(Message msg, PeerHandleContext ctx) {
        if (ctx.listener() != null) {
            ctx.listener().onEvent(msg.getType(), msg.getSenderId(), msg.getContent());
        }
        if ("PEER_JOINED".equals(msg.getType())) {
            tryResumeTransfers(msg, ctx);
        }
    }

    private void tryResumeTransfers(Message msg, PeerHandleContext ctx) {
        PeerInfo joined;
        try {
            joined = ctx.gson().fromJson(msg.getContent(), PeerInfo.class);
        } catch (JsonSyntaxException e) {
            return;
        }
        if (joined == null) return;

        List<FileTransferOutbound> pending =
                registry.findPendingOutboundByTarget(joined.getIpAddress(), joined.getPort());
        for (FileTransferOutbound transfer : pending) {
            System.out.println("[*] Peer " + joined.getPeerId() + " online — resume file: "
                    + transfer.getFileName() + " từ chunk " + transfer.getNextChunkIndex());
            registry.triggerResume(transfer);
        }
    }
}
