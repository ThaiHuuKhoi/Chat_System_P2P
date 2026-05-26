package org.khoicg.chat.peer.service;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.session.PeerSessionContext;
import org.khoicg.chat.util.MessageIdUtil;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Scanner;

public final class FileSendService {

    private static final long MAX_FILE_BYTES = 50L * 1024 * 1024; // 50 MB

    private final PeerSessionContext session;

    public FileSendService(PeerSessionContext session) {
        this.session = session;
    }

    /**
     * Gửi file không qua Scanner (dùng cho GUI).
     * @return "OK:fileName", "FILE_NOT_FOUND", "FILE_TOO_LARGE", "FAIL", hoặc "ERROR:..."
     */
    public String send(String targetIp, int targetPort, String filePath) {
        File file = new File(filePath);
        if (!file.exists() || file.isDirectory()) return "FILE_NOT_FOUND";
        if (file.length() > MAX_FILE_BYTES) return "FILE_TOO_LARGE";
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String base64Data = Base64.getEncoder().encodeToString(fileBytes);
            Message fileMsg = new Message("FILE", session.myId(), file.getName() + "||" + base64Data);
            fileMsg.setMessageId(MessageIdUtil.newId());
            String ack = session.messaging().sendReliable(targetIp, targetPort, fileMsg);
            return ack != null ? "OK:" + file.getName() : "FAIL";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
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

        System.out.print("Nhập đường dẫn tuyệt đối của file (VD: C:\\images\\anh.jpg): ");
        String filePath = scanner.nextLine();

        File file = new File(filePath);
        if (file.exists() && !file.isDirectory()) {
            if (file.length() > MAX_FILE_BYTES) {
                System.out.println("[-] File quá lớn — giới hạn 50 MB.");
                return;
            }
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String base64Data = Base64.getEncoder().encodeToString(fileBytes);
                String fileContent = file.getName() + "||" + base64Data;

                Message fileMsg = new Message("FILE", session.myId(), fileContent);
                fileMsg.setMessageId(MessageIdUtil.newId());

                System.out.println("[*] Đang gửi file " + file.getName() + " (" + (fileBytes.length / 1024) + " KB)...");
                String ack = session.messaging().sendReliable(targetIp, targetPort, fileMsg);

                if (ack != null) {
                    System.out.println("-> [Đã gửi file thành công!]");
                } else {
                    System.out.println("-> [Gửi thất bại: Người nhận không phản hồi]");
                }
            } catch (Exception e) {
                System.out.println("[-] Lỗi khi xử lý file: " + e.getMessage());
            }
        } else {
            System.out.println("[-] File không tồn tại hoặc đường dẫn sai!");
        }
    }
}
