package org.khoicg.chat.peer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.util.AESUtil;
import org.khoicg.chat.util.ReliableDeliveryHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PeerServer extends Thread {

    private final int myPort;
    private final String myPeerId;
    private final Gson gson = new Gson();

    private final ConcurrentHashMap<String, Boolean> seenDeliveries = new ConcurrentHashMap<>();

    public PeerServer(int port, String myPeerId) {
        this.myPort = port;
        this.myPeerId = myPeerId;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            System.out.println("[Hệ thống] Đang lắng nghe tin nhắn tại Port: " + myPort);

            while (true) {
                Socket incomingSocket = serverSocket.accept();
                new Thread(() -> handleIncomingMessage(incomingSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Lỗi Peer Server: " + e.getMessage());
        }
    }

    private void handleIncomingMessage(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String jsonInput = in.readLine();
            if (jsonInput != null) {
                Message msg = gson.fromJson(jsonInput, Message.class);

                if ("CHAT".equals(msg.getType()) || "GROUP_CHAT".equals(msg.getType())) {
                    boolean firstTime = markFirstDelivery(msg);
                    if (firstTime) {
                        String decryptedContent = AESUtil.decrypt(msg.getContent());
                        String prefix = "GROUP_CHAT".equals(msg.getType()) ? "[Tin nhắn Nhóm từ " : "[Tin nhắn từ ";
                        System.out.println("\n" + prefix + msg.getSenderId() + "]: " + decryptedContent);
                    } else {
                        System.out.println("\n[Bỏ qua tin trùng — cùng mã tin từ " + msg.getSenderId() + "]");
                    }
                    System.out.print("Chọn chức năng: ");
                    writeAck(out, msg, "Đã nhận");
                } else if ("RELAY_REGISTER".equals(msg.getType())) {
                    try {
                        PeerInfo joining = gson.fromJson(msg.getContent(), PeerInfo.class);
                        System.out.println("\n[Relay đăng ký] Nhận yêu cầu thay mặt peer: " + joining.getPeerId());
                        System.out.print("Chọn chức năng: ");
                    } catch (Exception ignored) { }
                    PeerClient pc = new PeerClient();
                    Message toTracker = new Message("REGISTER", msg.getSenderId(), msg.getContent());
                    String tr = pc.sendRequest(AppConfig.trackerHost(), AppConfig.trackerPort(), toTracker);
                    if (tr != null) {
                        out.println(tr);
                    } else {
                        out.println(gson.toJson(new Message("REGISTER_FAIL", "PeerServer", "Tracker không phản hồi")));
                    }
                } else if ("RELAY".equals(msg.getType())) {
                    handleRelay(msg, out);
                } else if ("FILE".equals(msg.getType())) {
                    try {
                        String[] parts = msg.getContent().split("\\|\\|");
                        if (parts.length == 2) {
                            String fileName = parts[0];
                            String base64Data = parts[1];

                            if (markFirstDelivery(msg)) {
                                byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Data);
                                java.io.File dir = new java.io.File("downloads_" + myPort);
                                if (!dir.exists()) {
                                    dir.mkdir();
                                }
                                java.io.File savedFile = new java.io.File(dir, fileName);
                                java.nio.file.Files.write(savedFile.toPath(), fileBytes);
                                System.out.println("\n[!] Đã nhận một file từ " + msg.getSenderId() + ": " + fileName);
                                System.out.println("[!] File được lưu tại: " + savedFile.getAbsolutePath());
                            } else {
                                System.out.println("\n[Bỏ qua file trùng — cùng mã tin từ " + msg.getSenderId() + "]");
                            }
                            System.out.print("Chọn chức năng: ");
                            writeAck(out, msg, "Đã nhận file");
                        }
                    } catch (Exception e) {
                        System.out.println("[-] Lỗi khi lưu file: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean markFirstDelivery(Message msg) {
        String mid = msg.getMessageId();
        if (mid == null || mid.isEmpty()) {
            return true;
        }
        String key = msg.getType() + "|" + msg.getSenderId() + "|" + mid;
        if (seenDeliveries.size() > 4096) {
            seenDeliveries.clear();
        }
        return seenDeliveries.putIfAbsent(key, Boolean.TRUE) == null;
    }

    private void writeAck(PrintWriter out, Message incoming, String ackText) {
        Message ack = new Message("ACK", "Server", ackText);
        ack.setMessageId(incoming.getMessageId());
        out.println(gson.toJson(ack));
    }

    private void writeAckFromPeer(PrintWriter out, Message incoming, String ackText) {
        Message ack = new Message("ACK", "PeerServer", ackText);
        ack.setMessageId(incoming.getMessageId());
        out.println(gson.toJson(ack));
    }

    private void handleRelay(Message msg, PrintWriter out) {
        String raw = msg.getContent();
        if (raw == null) {
            out.println(gson.toJson(new Message("RELAY_FAIL", "PeerServer", "Nội dung rỗng")));
            return;
        }
        String[] parts = raw.split("\\|\\|", 2);
        if (parts.length < 2) {
            out.println(gson.toJson(new Message("RELAY_FAIL", "PeerServer", "Sai định dạng RELAY")));
            return;
        }
        String targetId = parts[0].trim();
        String encrypted = parts[1];

        if (targetId.equals(myPeerId)) {
            String decrypted = AESUtil.decrypt(encrypted);
            System.out.println("\n[Tin relay từ " + msg.getSenderId() + "]: " + decrypted);
            System.out.print("Chọn chức năng: ");
            writeAckFromPeer(out, msg, "Đã nhận");
            return;
        }

        PeerClient pc = new PeerClient();
        String listJson = pc.sendRequest(AppConfig.trackerHost(), AppConfig.trackerPort(),
                new Message("GET_PEERS", myPeerId, ""));
        if (listJson == null) {
            out.println(gson.toJson(new Message("RELAY_FAIL", "PeerServer", "Không tới tracker")));
            return;
        }
        try {
            Message respMsg = gson.fromJson(listJson, Message.class);
            Type listType = new TypeToken<List<PeerInfo>>(){}.getType();
            List<PeerInfo> peers = gson.fromJson(respMsg.getContent(), listType);
            PeerInfo dest = null;
            if (peers != null) {
                for (PeerInfo p : peers) {
                    if (p.getPeerId().equals(targetId)) {
                        dest = p;
                        break;
                    }
                }
            }
            if (dest == null) {
                out.println(gson.toJson(new Message("RELAY_FAIL", "PeerServer", "Không tìm thấy đích trên tracker")));
                return;
            }

            System.out.println("[Relay] Chuyển tiếp tin của " + msg.getSenderId() + " → " + targetId);
            Message chat = new Message("CHAT", msg.getSenderId(), encrypted);
            chat.setMessageId(msg.getMessageId());
            String ack = pc.sendReliableRequest(dest.getIpAddress(), dest.getPort(), chat);
            if (ReliableDeliveryHelper.ackJsonMatches(ack, chat.getMessageId())) {
                writeAckFromPeer(out, msg, "Đã chuyển tiếp");
                return;
            }

            Message store = new Message("STORE_OFFLINE", msg.getSenderId(), targetId + "||" + encrypted);
            store.setMessageId(msg.getMessageId());
            String sresp = pc.sendRequest(AppConfig.trackerHost(), AppConfig.trackerPort(), store);
            boolean stored = false;
            if (sresp != null) {
                try {
                    Message sm = gson.fromJson(sresp, Message.class);
                    stored = sm != null && "ACK".equals(sm.getType());
                } catch (Exception ignored) { }
            }
            if (stored) {
                writeAckFromPeer(out, msg, "Đích offline — tracker giữ hộ");
            } else {
                out.println(gson.toJson(new Message("RELAY_FAIL", "PeerServer", "Chuyển tiếp thất bại")));
            }
        } catch (Exception e) {
            out.println(gson.toJson(new Message("RELAY_FAIL", "PeerServer", e.getMessage())));
        }
    }
}
