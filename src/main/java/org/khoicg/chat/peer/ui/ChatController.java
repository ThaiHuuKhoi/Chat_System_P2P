package org.khoicg.chat.peer.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.google.gson.Gson;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.PeerServer;
import org.khoicg.chat.peer.service.PeerApplicationServices;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatController {

    // Top bar
    @FXML private Label userInfoLabel;

    // Peer list
    @FXML private ListView<PeerInfo> peerListView;

    // Message log
    @FXML private TextArea messageLogArea;

    // Direct chat tab
    @FXML private TextField directIpField;
    @FXML private TextField directPortField;
    @FXML private TextField directMessageField;

    // Group chat tab
    @FXML private TextField groupTargetsField;
    @FXML private TextField groupMessageField;

    // File send tab
    @FXML private TextField fileIpField;
    @FXML private TextField filePortField;
    @FXML private TextField filePathField;

    // Relay tab
    @FXML private TextField relayIpField;
    @FXML private TextField relayPortField;
    @FXML private TextField relayTargetField;
    @FXML private TextField relayMessageField;

    private PeerSessionContext session;
    private PeerApplicationServices services;
    private AtomicBoolean running;

    private final ObservableList<PeerInfo> peerItems = FXCollections.observableArrayList();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        peerListView.setItems(peerItems);
        peerListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PeerInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : "● " + item.getPeerId() + "\n   " + item.getIpAddress() + ":" + item.getPort());
            }
        });

        // Click peer → autofill IP/Port in Direct + File tabs
        peerListView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                directIpField.setText(sel.getIpAddress());
                directPortField.setText(String.valueOf(sel.getPort()));
                fileIpField.setText(sel.getIpAddress());
                filePortField.setText(String.valueOf(sel.getPort()));
            }
        });
    }

    /**
     * Gọi từ LoginController sau khi đăng nhập thành công.
     */
    public void init(String myId, int myPort,
                     PeerSessionContext session, PeerApplicationServices services,
                     AtomicBoolean running, PeerServer server,
                     List<String> offlineMsgs) {
        this.session = session;
        this.services = services;
        this.running = running;

        userInfoLabel.setText("● " + myId + "   |   port " + myPort);

        // Gắn listener để nhận tin nhắn đến từ thread PeerServer
        Gson gson = new Gson();
        server.setMessageListener((type, senderId, content) -> Platform.runLater(() -> {
            switch (type) {
                case "CHAT"       -> appendLog("[" + senderId + " → bạn]: " + content);
                case "GROUP_CHAT" -> appendLog("[Nhóm | " + senderId + "]: " + content);
                case "FILE"       -> appendLog("[FILE từ " + senderId + "]: " + content);
                case "PEER_JOINED" -> {
                    PeerInfo p = gson.fromJson(content, PeerInfo.class);
                    if (p != null && !p.getPeerId().equals(session.myId())
                            && peerItems.stream().noneMatch(x -> x.getPeerId().equals(p.getPeerId()))) {
                        peerItems.add(p);
                        appendLog("[+] " + p.getPeerId() + " vừa online");
                    }
                }
                case "PEER_LEFT" -> {
                    peerItems.removeIf(p -> p.getPeerId().equals(content));
                    appendLog("[-] " + content + " vừa offline");
                }
                default -> appendLog("[" + senderId + "]: " + content);
            }
        }));

        // Hiển thị tin nhắn offline nếu có
        if (!offlineMsgs.isEmpty()) {
            appendLog("── " + offlineMsgs.size() + " tin nhắn khi bạn offline ──");
            offlineMsgs.forEach(m -> appendLog("[Offline] " + m));
            appendLog("────────────────────────────────────");
        }

        appendLog("[Hệ thống] Đã kết nối thành công với tư cách: " + myId + " @ :" + myPort);
        handleRefreshPeers();

        // Gửi QUIT về Tracker khi người dùng đóng cửa sổ
        Stage stage = (Stage) userInfoLabel.getScene().getWindow();
        stage.setOnCloseRequest(e -> doExit());
    }

    // ─── PEER LIST ──────────────────────────────────────────────────────────────

    @FXML
    private void handleRefreshPeers() {
        new Thread(() -> {
            services.directory.invalidateCache();
            List<PeerInfo> peers = services.directory.fetchOnlinePeers();
            Platform.runLater(() -> {
                peerItems.clear();
                if (peers != null) {
                    peers.stream()
                         .filter(p -> !p.getPeerId().equals(session.myId()))
                         .forEach(peerItems::add);
                }
            });
        }).start();
    }

    // ─── CHAT TRỰC TIẾP ────────────────────────────────────────────────────────

    @FXML
    private void handleSendDirect() {
        String ip = directIpField.getText().trim();
        String portStr = directPortField.getText().trim();
        String message = directMessageField.getText().trim();

        if (ip.isEmpty() || portStr.isEmpty() || message.isEmpty()) {
            appendLog("[!] Điền đầy đủ IP, Port và nội dung tin nhắn.");
            return;
        }
        int port;
        try { port = Integer.parseInt(portStr); }
        catch (NumberFormatException e) { appendLog("[!] Port không hợp lệ."); return; }

        directMessageField.clear();
        appendLog("[Bạn → " + ip + ":" + port + "]: " + message);

        new Thread(() -> {
            String result = services.directChat.send(ip, port, message);
            Platform.runLater(() -> {
                if ("OK".equals(result)) {
                    appendLog("[✓] Đã gửi và xác nhận.");
                } else if ("OFFLINE_STORED".equals(result)) {
                    appendLog("[📥] Peer offline — tin đã lưu ở Tracker, sẽ giao khi họ online.");
                } else {
                    appendLog("[✗] Gửi thất bại: peer không phản hồi và không tìm thấy ID trên Tracker.");
                }
            });
        }).start();
    }

    // ─── CHAT NHÓM ──────────────────────────────────────────────────────────────

    @FXML
    private void handleSendGroup() {
        String targets = groupTargetsField.getText().trim();
        String message = groupMessageField.getText().trim();

        if (targets.isEmpty() || message.isEmpty()) {
            appendLog("[!] Điền Targets và nội dung tin nhắn.");
            return;
        }

        groupMessageField.clear();
        String label = targets.equalsIgnoreCase("ALL") ? "Broadcast" : "Nhóm [" + targets + "]";
        appendLog("[Bạn → " + label + "]: " + message);

        new Thread(() -> {
            var r = services.groupChat.send(targets, message);
            Platform.runLater(() ->
                appendLog("[✓] " + label + ": " + r.successCount() + " thành công"
                    + (r.storedCount() > 0 ? ", " + r.storedCount() + " lưu offline" : "")
                    + (r.failCount() > 0 ? ", " + r.failCount() + " thất bại" : "") + "."));
        }).start();
    }

    // ─── GỬI FILE ───────────────────────────────────────────────────────────────

    @FXML
    private void handleBrowseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn file để gửi");
        File file = chooser.showOpenDialog(filePathField.getScene().getWindow());
        if (file != null) filePathField.setText(file.getAbsolutePath());
    }

    @FXML
    private void handleSendFile() {
        String ip = fileIpField.getText().trim();
        String portStr = filePortField.getText().trim();
        String filePath = filePathField.getText().trim();

        if (ip.isEmpty() || portStr.isEmpty() || filePath.isEmpty()) {
            appendLog("[!] Điền đầy đủ IP, Port và chọn file.");
            return;
        }
        int port;
        try { port = Integer.parseInt(portStr); }
        catch (NumberFormatException e) { appendLog("[!] Port không hợp lệ."); return; }

        appendLog("[File → " + ip + ":" + port + "] Đang gửi...");

        new Thread(() -> {
            String result = services.fileSend.send(ip, port, filePath);
            Platform.runLater(() -> {
                if (result.startsWith("OK:")) {
                    appendLog("[✓] Gửi file thành công: " + result.substring(3));
                } else if ("FILE_NOT_FOUND".equals(result)) {
                    appendLog("[✗] File không tồn tại hoặc đường dẫn sai.");
                } else if ("FILE_TOO_LARGE".equals(result)) {
                    appendLog("[✗] File quá lớn — giới hạn 50 MB.");
                } else {
                    appendLog("[✗] Gửi file thất bại: " + result);
                }
            });
        }).start();
    }

    // ─── RELAY ──────────────────────────────────────────────────────────────────

    @FXML
    private void handleSendRelay() {
        String rIp = relayIpField.getText().trim();
        String rPortStr = relayPortField.getText().trim();
        String targetId = relayTargetField.getText().trim();
        String message = relayMessageField.getText().trim();

        if (rIp.isEmpty() || rPortStr.isEmpty() || targetId.isEmpty() || message.isEmpty()) {
            appendLog("[!] Điền đầy đủ Relay IP, Port, Target ID và nội dung.");
            return;
        }
        int rPort;
        try { rPort = Integer.parseInt(rPortStr); }
        catch (NumberFormatException e) { appendLog("[!] Port relay không hợp lệ."); return; }

        relayMessageField.clear();
        appendLog("[Bạn →(relay " + rIp + ":" + rPort + ")→ " + targetId + "]: " + message);

        new Thread(() -> {
            String result = services.relaySend.send(rIp, rPort, targetId, message);
            Platform.runLater(() -> {
                if (result.startsWith("OK:")) {
                    appendLog("[✓] Relay thành công: " + result.substring(3));
                } else if (result.startsWith("RELAY_FAIL:")) {
                    appendLog("[✗] Relay thất bại: " + result.substring(11));
                } else {
                    appendLog("[✗] Relay không có phản hồi: " + result);
                }
            });
        }).start();
    }

    // ─── EXIT ───────────────────────────────────────────────────────────────────

    @FXML
    private void handleExit() {
        doExit();
    }

    private void doExit() {
        running.set(false);
        new Thread(() -> {
            try {
                session.messaging().sendTracker(new Message("QUIT", session.myId(), ""));
            } catch (Exception ignored) {}
            Platform.runLater(() -> {
                Stage stage = (Stage) userInfoLabel.getScene().getWindow();
                stage.close();
            });
        }).start();
    }

    // ─── HELPER ─────────────────────────────────────────────────────────────────

    private void appendLog(String message) {
        String time = LocalTime.now().format(TIME_FMT);
        messageLogArea.appendText("[" + time + "]  " + message + "\n");
    }
}
