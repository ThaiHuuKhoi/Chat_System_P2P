package org.khoicg.chat.peer.handler;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.PeerHandleContext;
import org.khoicg.chat.peer.PeerInboundHandler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public final class FileInboundHandler implements PeerInboundHandler {

    @Override
    public boolean supports(String type) {
        return "FILE".equals(type);
    }

    @Override
    public void handle(Message msg, PeerHandleContext ctx) {
        try {
            String[] parts = msg.getContent().split("\\|\\|", 2);
            if (parts.length != 2) {
                return;
            }
            // Chỉ giữ tên file, bỏ mọi path component để ngăn path traversal
            String fileName = Paths.get(parts[0]).getFileName().toString();
            String base64Data = parts[1];

            if (ctx.deduper().markFirstDelivery(msg)) {
                byte[] fileBytes = Base64.getDecoder().decode(base64Data);
                File dir = new File("downloads_" + ctx.myPort());
                if (!dir.exists()) {
                    dir.mkdir();
                }
                File savedFile = new File(dir, fileName);
                Files.write(savedFile.toPath(), fileBytes);
                System.out.println("\n[!] Đã nhận một file từ " + msg.getSenderId() + ": " + fileName);
                System.out.println("[!] File được lưu tại: " + savedFile.getAbsolutePath());
                if (ctx.listener() != null) {
                    ctx.listener().onEvent("FILE", msg.getSenderId(), fileName + " → " + savedFile.getAbsolutePath());
                }
            } else {
                System.out.println("\n[Bỏ qua file trùng — cùng mã tin từ " + msg.getSenderId() + "]");
            }
            if (ctx.listener() == null) System.out.print("Chọn chức năng: ");
            ctx.writeAck(msg, "Đã nhận file");
        } catch (Exception e) {
            System.out.println("[-] Lỗi khi lưu file: " + e.getMessage());
        }
    }
}
