package org.khoicg.chat.tracker;

import com.google.gson.Gson;
import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TrackerServer {

    private static final ConcurrentHashMap<String, PeerInfo> onlinePeers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastSeenPeers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<Message>> offlineMessages = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = AppConfig.trackerPort();
        System.out.println("=== Tracker Server (bootstrap) — port " + port + " ===");

        startHeartbeatMonitor();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new TrackerHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startHeartbeatMonitor() {
        Thread monitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000);
                    long currentTime = System.currentTimeMillis();

                    for (String peerId : lastSeenPeers.keySet()) {
                        if (currentTime - lastSeenPeers.get(peerId) > 15000) {
                            onlinePeers.remove(peerId);
                            lastSeenPeers.remove(peerId);
                            System.out.println("[-] Phát hiện Peer rớt mạng (Timeout): Đã xóa " + peerId);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        monitor.setDaemon(true);
        monitor.start();
    }

    static class TrackerHandler extends Thread {
        private Socket socket;
        private Gson gson = new Gson();

        public TrackerHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String jsonInput = in.readLine();
                if (jsonInput == null) return;

                Message msg = gson.fromJson(jsonInput, Message.class);

                if (msg.getSenderId() != null) {
                    lastSeenPeers.put(msg.getSenderId(), System.currentTimeMillis());
                }

                if ("REGISTER".equals(msg.getType())) {
                    PeerInfo newPeer = gson.fromJson(msg.getContent(), PeerInfo.class);
                    onlinePeers.put(newPeer.getPeerId(), newPeer);
                    lastSeenPeers.put(newPeer.getPeerId(), System.currentTimeMillis());

                    System.out.println("[+] Mới gia nhập: " + newPeer);
                    Message response = new Message("REGISTER_OK", "Tracker", "Thành công!");
                    out.println(gson.toJson(response));

                } else if ("GET_PEERS".equals(msg.getType())) {
                    String peerListJson = gson.toJson(onlinePeers.values());
                    Message response = new Message("PEER_LIST", "Tracker", peerListJson);
                    out.println(gson.toJson(response));
                    System.out.println("[*] Đã gửi danh bạ cho: " + msg.getSenderId());

                } else if ("HEARTBEAT".equals(msg.getType())) {
                    Message response = new Message("HEARTBEAT_OK", "Tracker", "");
                    out.println(gson.toJson(response));

                } else if ("QUIT".equals(msg.getType())) {
                    onlinePeers.remove(msg.getSenderId());
                    lastSeenPeers.remove(msg.getSenderId());
                    System.out.println("[-] Peer chủ động thoát: " + msg.getSenderId());
                    Message response = new Message("QUIT_OK", "Tracker", "Đã xóa khỏi hệ thống");
                    out.println(gson.toJson(response));
                } else if ("STORE_OFFLINE".equals(msg.getType())) {
                    String[] parts = msg.getContent().split("\\|\\|", 2);
                    if (parts.length == 2) {
                        String targetId = parts[0];
                        String encryptedContent = parts[1];

                        Message storedMsg = new Message("OFFLINE_CHAT", msg.getSenderId(), encryptedContent);
                        if (msg.getMessageId() != null && !msg.getMessageId().isEmpty()) {
                            storedMsg.setMessageId(msg.getMessageId());
                        }

                        offlineMessages.computeIfAbsent(targetId, k -> new ArrayList<>()).add(storedMsg);

                        System.out.println("[*] Nhận giữ hộ 1 tin nhắn cho: " + targetId);
                        Message response = new Message("ACK", "Tracker", "Đã lưu");
                        out.println(gson.toJson(response));
                    }

                } else if ("PULL_OFFLINE".equals(msg.getType())) {
                    List<Message> msgs = offlineMessages.remove(msg.getSenderId());

                    if (msgs != null && !msgs.isEmpty()) {
                        Message response = new Message("OFFLINE_MESSAGES", "Tracker", gson.toJson(msgs));
                        out.println(gson.toJson(response));
                        System.out.println("[*] Đã giao " + msgs.size() + " tin nhắn vắng mặt cho: " + msg.getSenderId());
                    } else {
                        Message response = new Message("NO_OFFLINE", "Tracker", "");
                        out.println(gson.toJson(response));
                    }
                }

            } catch (IOException e) {
                // ignore
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
