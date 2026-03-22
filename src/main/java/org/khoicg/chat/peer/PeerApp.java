package org.khoicg.chat.peer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.util.AESUtil;
import org.khoicg.chat.util.MessageIdUtil;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class PeerApp {

    private static final String TRACKER_IP = AppConfig.trackerHost();
    private static final int TRACKER_PORT = AppConfig.trackerPort();
    private static final Gson gson = new Gson();

    private static volatile boolean isRunning = true;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        PeerClient client = new PeerClient();

        System.out.println("===============================");
        System.out.println("    CHÀO MỪNG ĐẾN P2P CHAT     ");
        System.out.println("===============================");

        System.out.print("Nhập Tên của bạn (Peer ID): ");
        String myId = scanner.nextLine();

        System.out.print("Nhập Port để nhận tin nhắn (vd: 5001, 5002...): ");
        int myPort = Integer.parseInt(scanner.nextLine());

        PeerServer server = new PeerServer(myPort, myId);
        server.start();

        PeerInfo myInfo = new PeerInfo(myId, AppConfig.peerAdvertiseHost(), myPort);
        Message regMsg = new Message("REGISTER", myId, gson.toJson(myInfo));

        System.out.print("\nTham gia mạng — (1) Tracker trực tiếp  (2) Qua peer đã biết (relay đăng ký): ");
        String joinMode = scanner.nextLine().trim();

        String regResponseJson;
        if ("2".equals(joinMode)) {
            System.out.print("IP peer đã biết: ");
            String knownIp = scanner.nextLine();
            System.out.print("Port peer đã biết: ");
            int knownPort = Integer.parseInt(scanner.nextLine());
            System.out.println("\n[*] Đang nhờ peer " + knownIp + ":" + knownPort + " đăng ký hộ với Tracker...");
            Message relayReg = new Message("RELAY_REGISTER", myId, gson.toJson(myInfo));
            regResponseJson = client.sendRequest(knownIp, knownPort, relayReg);
        } else {
            System.out.println("\n[*] Đang kết nối tới Tracker Server (" + TRACKER_IP + ":" + TRACKER_PORT + ")...");
            regResponseJson = client.sendRequest(TRACKER_IP, TRACKER_PORT, regMsg);
        }

        Message regParsed = regResponseJson != null ? gson.fromJson(regResponseJson, Message.class) : null;
        boolean registerOk = regParsed != null && "REGISTER_OK".equals(regParsed.getType());

        if (registerOk) {
            System.out.println("[+] " + regParsed.getContent());

            startHeartbeatSender(myId, client);

            Message pullMsg = new Message("PULL_OFFLINE", myId, "");
            String pullResponse = client.sendRequest(TRACKER_IP, TRACKER_PORT, pullMsg);
            if (pullResponse != null) {
                Message respMsg = gson.fromJson(pullResponse, Message.class);
                if ("OFFLINE_MESSAGES".equals(respMsg.getType())) {
                    Type msgListType = new TypeToken<List<Message>>(){}.getType();
                    List<Message> missedMsgs = gson.fromJson(respMsg.getContent(), msgListType);

                    System.out.println("\n🔔 BẠN CÓ " + missedMsgs.size() + " TIN NHẮN KHI ĐANG OFFLINE:");
                    Set<String> seenPullKeys = new HashSet<>();
                    for (Message m : missedMsgs) {
                        String mid = m.getMessageId();
                        if (mid != null && !mid.isEmpty()) {
                            String key = m.getSenderId() + "|" + mid;
                            if (!seenPullKeys.add(key)) {
                                continue;
                            }
                        }
                        String dec = AESUtil.decrypt(m.getContent());
                        System.out.println("   -> [Gửi từ " + m.getSenderId() + "]: " + dec);
                    }
                    System.out.println("----------------------------------------");
                }
            }

        } else {
            if (regParsed != null && "REGISTER_FAIL".equals(regParsed.getType())) {
                System.out.println("[-] Peer relay không đăng ký được với Tracker: " + regParsed.getContent());
            } else if ("2".equals(joinMode)) {
                System.out.println("[-] Không gửi được RELAY_REGISTER tới peer đã biết (peer tắt / sai IP:port).");
            } else {
                System.out.println("[-] Không thể kết nối tới Tracker. Vui lòng bật TrackerServer trước!");
            }
            System.exit(0);
        }

        while (isRunning) {
            System.out.println("\n--- MENU ---");
            System.out.println("1. Xem danh sách đang Online");
            System.out.println("2. Nhắn tin trực tiếp (P2P)");
            System.out.println("3. Chat Nhóm / Broadcast toàn mạng");
            System.out.println("4. Gửi File (P2P)");
            System.out.println("5. Gửi tin qua peer trung gian (relay)");
            System.out.println("6. Thoát");
            System.out.print("Chọn chức năng: ");
            String choice = scanner.nextLine();

            if ("1".equals(choice)) {
                Message getMsg = new Message("GET_PEERS", myId, "");
                String jsonResponse = client.sendRequest(TRACKER_IP, TRACKER_PORT, getMsg);

                if (jsonResponse != null) {
                    Message respMsg = gson.fromJson(jsonResponse, Message.class);
                    Type listType = new TypeToken<List<PeerInfo>>(){}.getType();
                    List<PeerInfo> peers = gson.fromJson(respMsg.getContent(), listType);

                    System.out.println("\n--- DANH SÁCH ONLINE ---");
                    for (PeerInfo p : peers) {
                        System.out.println("- " + p.getPeerId() + " (IP: " + p.getIpAddress() + ", Port: " + p.getPort() + ")");
                    }
                }

            } else if ("2".equals(choice)) {
                System.out.print("Nhập IP người nhận: ");
                String targetIp = scanner.nextLine();
                System.out.print("Nhập Port người nhận: ");
                int targetPort = Integer.parseInt(scanner.nextLine());
                System.out.print("Nhập nội dung tin nhắn: ");
                String content = scanner.nextLine();

                String encryptedContent = AESUtil.encrypt(content);
                System.out.println("Encrypted: " + encryptedContent);
                String msgId = MessageIdUtil.newId();
                Message chatMsg = new Message("CHAT", myId, encryptedContent);
                chatMsg.setMessageId(msgId);

                String ack = client.sendReliableRequest(targetIp, targetPort, chatMsg);

                if (ack != null) {
                    System.out.println("-> [Đã gửi và người nhận ĐÃ XÁC NHẬN (msgId=" + msgId + ")]");
                } else {
                    System.out.println("-> [GỬI THẤT BẠI: Tin nhắn không tới được đích]");
                    if (encryptedContent != null) {
                        List<PeerInfo> peers = fetchOnlinePeers(client, myId);
                        String targetPeerId = resolvePeerIdByAddress(peers, targetIp, targetPort);
                        if (targetPeerId != null) {
                            if (tryStoreOffline(client, myId, targetPeerId, encryptedContent, msgId)) {
                                System.out.println("-> [Tracker đã lưu tin; " + targetPeerId + " sẽ nhận khi online lại]");
                            } else {
                                System.out.println("-> [Không lưu được tin lên tracker]");
                            }
                        } else {
                            System.out.println("-> [Không lưu offline: IP/Port không khớp peer nào trên tracker — xem menu 1]");
                        }
                    }
                }

            } else if ("3".equals(choice)) {
                Message getMsg = new Message("GET_PEERS", myId, "");
                String jsonResponse = client.sendRequest(TRACKER_IP, TRACKER_PORT, getMsg);

                if (jsonResponse != null) {
                    Message respMsg = gson.fromJson(jsonResponse, Message.class);
                    Type listType = new TypeToken<List<PeerInfo>>(){}.getType();
                    List<PeerInfo> peers = gson.fromJson(respMsg.getContent(), listType);

                    System.out.print("Nhập ID những người muốn chat (cách nhau bằng dấu phẩy), hoặc gõ 'ALL' để gửi toàn mạng: ");
                    String targets = scanner.nextLine();
                    System.out.print("Nhập nội dung tin nhắn nhóm: ");
                    String content = scanner.nextLine();

                    String encryptedContent = AESUtil.encrypt(content);
                    System.out.println("Đang gửi đi chuỗi mã hóa: " + encryptedContent);
                    String batchMsgId = MessageIdUtil.newId();
                    Message groupMsg = new Message("GROUP_CHAT", myId, encryptedContent);
                    groupMsg.setMessageId(batchMsgId);

                    boolean isBroadcast = targets.equalsIgnoreCase("ALL");
                    String[] targetIds = targets.split(",");

                    int successCount = 0;
                    System.out.println("--- ĐANG GỬI TIN NHÓM ---");

                    for (PeerInfo p : peers) {
                        if (p.getPeerId().equals(myId)) continue;

                        boolean shouldSend = isBroadcast;
                        if (!isBroadcast) {
                            for (String t : targetIds) {
                                if (p.getPeerId().equalsIgnoreCase(t.trim())) {
                                    shouldSend = true;
                                    break;
                                }
                            }
                        }

                        if (shouldSend) {
                            System.out.print("Gửi tới " + p.getPeerId() + "... ");
                            String ack = client.sendReliableRequest(p.getIpAddress(), p.getPort(), groupMsg);
                            if (ack != null) {
                                System.out.println("[Thành công]");
                                successCount++;
                            } else {
                                System.out.println("[Thất bại]");
                                if (encryptedContent != null
                                        && tryStoreOffline(client, myId, p.getPeerId(), encryptedContent, batchMsgId)) {
                                    System.out.println("    -> Tracker giữ hộ tin cho " + p.getPeerId());
                                }
                            }
                        }
                    }
                    System.out.println("-> [Hoàn tất! Đã gửi thành công tới " + successCount + " peer]");
                } else {
                    System.out.println("[-] Không lấy được danh bạ từ Tracker.");
                }

            } else if ("4".equals(choice)) {
                System.out.print("Nhập IP người nhận: ");
                String targetIp = scanner.nextLine();
                System.out.print("Nhập Port người nhận: ");
                int targetPort = Integer.parseInt(scanner.nextLine());

                System.out.print("Nhập đường dẫn tuyệt đối của file (VD: C:\\images\\anh.jpg): ");
                String filePath = scanner.nextLine();

                java.io.File file = new java.io.File(filePath);
                if (file.exists() && !file.isDirectory()) {
                    try {
                        byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
                        String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
                        String fileContent = file.getName() + "||" + base64Data;

                        Message fileMsg = new Message("FILE", myId, fileContent);
                        fileMsg.setMessageId(MessageIdUtil.newId());

                        System.out.println("[*] Đang gửi file " + file.getName() + " (" + (fileBytes.length / 1024) + " KB)...");
                        String ack = client.sendReliableRequest(targetIp, targetPort, fileMsg);

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

            } else if ("5".equals(choice)) {
                System.out.print("IP peer trung gian (relay): ");
                String relayIp = scanner.nextLine();
                System.out.print("Port peer trung gian: ");
                int relayPort = Integer.parseInt(scanner.nextLine());
                System.out.print("Peer ID người nhận cuối: ");
                String finalTargetId = scanner.nextLine().trim();
                System.out.print("Nội dung tin nhắn: ");
                String relayText = scanner.nextLine();

                String encRelay = AESUtil.encrypt(relayText);
                if (encRelay == null) {
                    System.out.println("[-] Lỗi mã hóa.");
                } else {
                    Message relayMsg = new Message("RELAY", myId, finalTargetId + "||" + encRelay);
                    relayMsg.setMessageId(MessageIdUtil.newId());
                    String relayAck = client.sendReliableRequest(relayIp, relayPort, relayMsg);
                    if (relayAck != null) {
                        try {
                            Message rm = gson.fromJson(relayAck, Message.class);
                            if ("ACK".equals(rm.getType())) {
                                System.out.println("-> [Relay thành công: " + rm.getContent() + "]");
                            } else if ("RELAY_FAIL".equals(rm.getType())) {
                                System.out.println("-> [Relay thất bại: " + rm.getContent() + "]");
                            } else {
                                System.out.println("-> [Phản hồi: " + relayAck + "]");
                            }
                        } catch (Exception e) {
                            System.out.println("-> [Phản hồi: " + relayAck + "]");
                        }
                    } else {
                        System.out.println("-> [Không có ACK hợp lệ từ peer trung gian sau các lần thử]");
                    }
                }

            } else if ("6".equals(choice)) {
                System.out.println("Đang thông báo cho Tracker và thoát hệ thống...");
                Message quitMsg = new Message("QUIT", myId, "");
                client.sendRequest(TRACKER_IP, TRACKER_PORT, quitMsg);
                isRunning = false;
                System.exit(0);
            } else {
                System.out.println("Lựa chọn không hợp lệ!");
            }
        }
    }

    private static List<PeerInfo> fetchOnlinePeers(PeerClient client, String myId) {
        String jsonResponse = client.sendRequest(TRACKER_IP, TRACKER_PORT, new Message("GET_PEERS", myId, ""));
        if (jsonResponse == null) return null;
        try {
            Message respMsg = gson.fromJson(jsonResponse, Message.class);
            Type listType = new TypeToken<List<PeerInfo>>(){}.getType();
            return gson.fromJson(respMsg.getContent(), listType);
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolvePeerIdByAddress(List<PeerInfo> peers, String ip, int port) {
        if (peers == null) return null;
        String n = normalizeHost(ip);
        for (PeerInfo p : peers) {
            if (p.getPort() != port) continue;
            if (normalizeHost(p.getIpAddress()).equals(n)) return p.getPeerId();
        }
        return null;
    }

    private static String normalizeHost(String host) {
        if (host == null) return "";
        String h = host.trim().toLowerCase();
        if ("127.0.0.1".equals(h)) return "localhost";
        return h;
    }

    private static boolean tryStoreOffline(PeerClient client, String senderId, String targetPeerId,
                                         String encryptedContent, String messageId) {
        String payload = targetPeerId + "||" + encryptedContent;
        Message store = new Message("STORE_OFFLINE", senderId, payload);
        if (messageId != null && !messageId.isEmpty()) {
            store.setMessageId(messageId);
        }
        String resp = client.sendRequest(TRACKER_IP, TRACKER_PORT, store);
        if (resp == null) return false;
        try {
            Message m = gson.fromJson(resp, Message.class);
            return "ACK".equals(m.getType());
        } catch (Exception e) {
            return false;
        }
    }

    private static void startHeartbeatSender(String myId, PeerClient client) {
        Thread heartbeatThread = new Thread(() -> {
            while (isRunning) {
                try {
                    Thread.sleep(5000);
                    Message pingMsg = new Message("HEARTBEAT", myId, "ping");
                    client.sendRequest(TRACKER_IP, TRACKER_PORT, pingMsg);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    // tracker down
                }
            }
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }
}
