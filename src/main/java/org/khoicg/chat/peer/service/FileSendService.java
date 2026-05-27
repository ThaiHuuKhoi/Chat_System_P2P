package org.khoicg.chat.peer.service;

import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.model.Message;

import org.khoicg.chat.peer.session.PeerSessionContext;
import org.khoicg.chat.util.MessageIdUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Base64;
import java.util.Scanner;

public final class FileSendService {


    private final PeerSessionContext session;
    private final FileTransferRegistry registry;

    public FileSendService(PeerSessionContext session, FileTransferRegistry registry) {
        this.session  = session;
        this.registry = registry;
        registry.setResumeCallback(this::doSend);
    }

    /**
     * Gửi file mới (dùng cho GUI).
     * @return "OK:fileName", "FILE_NOT_FOUND", "FILE_TOO_LARGE", "INTERRUPTED", "ERROR:..."
     */
    public String send(String targetIp, int targetPort, String filePath) {
        File file = new File(filePath);
        if (!file.exists() || file.isDirectory()) return "FILE_NOT_FOUND";
        if (file.length() > AppConfig.peerFileMaxBytes()) return "FILE_TOO_LARGE";

        int chunkSize   = AppConfig.chunkSizeBytes();
        int totalChunks = (int) Math.ceil((double) file.length() / chunkSize);
        if (totalChunks == 0) totalChunks = 1;

        FileTransferOutbound transfer = new FileTransferOutbound(
                MessageIdUtil.newId(), targetIp, targetPort, filePath, file.getName(), totalChunks);
        registry.registerOutbound(transfer);
        return doSend(transfer);
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
        System.out.print("Nhập đường dẫn tuyệt đối của file: ");
        String filePath = scanner.nextLine();

        String result = send(targetIp, targetPort, filePath);
        if (result.startsWith("OK:")) {
            System.out.println("-> [Đã gửi file thành công: " + result.substring(3) + "]");
        } else if ("INTERRUPTED".equals(result)) {
            System.out.println("-> [Gửi bị ngắt — sẽ tiếp tục khi peer online lại]");
        } else {
            System.out.println("-> [Gửi thất bại: " + result + "]");
        }
    }

    // ─── internal ────────────────────────────────────────────────────────────────

    private String doSend(FileTransferOutbound transfer) {
        File file = new File(transfer.getFilePath());
        if (!file.exists()) {
            registry.removeOutbound(transfer.getTransferId());
            return "FILE_NOT_FOUND";
        }

        // Khi resume: hỏi receiver đã nhận đến chunk nào
        if (transfer.getNextChunkIndex() > 0) {
            int receiverNext = queryReceiverProgress(transfer);
            transfer.setNextChunkIndex(receiverNext);
        }

        int chunkSize = AppConfig.chunkSizeBytes();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long fileLen = raf.length();
            for (int i = transfer.getNextChunkIndex(); i < transfer.getTotalChunks(); i++) {
                long offset     = (long) i * chunkSize;
                int  actualSize = (int) Math.min(chunkSize, fileLen - offset);
                byte[] buf      = new byte[actualSize];
                raf.seek(offset);
                raf.readFully(buf);

                String payload = transfer.getTransferId() + "||"
                        + transfer.getFileName()  + "||"
                        + i                       + "||"
                        + transfer.getTotalChunks() + "||"
                        + Base64.getEncoder().encodeToString(buf);

                Message msg = new Message("FILE_CHUNK", session.myId(), payload);
                msg.setMessageId(MessageIdUtil.newId());

                String ack = session.messaging().sendReliable(
                        transfer.getTargetIp(), transfer.getTargetPort(), msg);
                if (ack == null) {
                    transfer.setNextChunkIndex(i);
                    System.out.println("[!] Gửi bị ngắt tại chunk " + i + "/" + transfer.getTotalChunks()
                            + " — sẽ tiếp tục khi " + transfer.getTargetIp() + " online lại");
                    return "INTERRUPTED";
                }
                transfer.setNextChunkIndex(i + 1);
            }
        } catch (IOException e) {
            return "ERROR:" + e.getMessage();
        }

        registry.removeOutbound(transfer.getTransferId());
        System.out.println("[+] Gửi file hoàn tất: " + transfer.getFileName());
        return "OK:" + transfer.getFileName();
    }

    private int queryReceiverProgress(FileTransferOutbound transfer) {
        Message req = new Message("FILE_RESUME_REQ", session.myId(), transfer.getTransferId());
        req.setMessageId(MessageIdUtil.newId());
        String ack = session.messaging().sendReliable(
                transfer.getTargetIp(), transfer.getTargetPort(), req);
        if (ack == null) return transfer.getNextChunkIndex();
        try {
            Message ackMsg = session.gson().fromJson(ack, Message.class);
            return Integer.parseInt(ackMsg.getContent().trim());
        } catch (Exception ignored) {
            return transfer.getNextChunkIndex();
        }
    }
}
