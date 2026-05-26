package org.khoicg.chat.peer.ui;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.PeerClient;
import org.khoicg.chat.peer.PeerMessagingAdapter;
import org.khoicg.chat.peer.PeerServer;
import org.khoicg.chat.peer.service.PeerApplicationServices;
import org.khoicg.chat.peer.service.PeerJoinService;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.concurrent.atomic.AtomicBoolean;

public class LoginController {

    @FXML private TextField peerIdField;
    @FXML private TextField portField;
    @FXML private RadioButton directRadio;
    @FXML private RadioButton relayRadio;
    @FXML private VBox relayBox;
    @FXML private TextField relayIpField;
    @FXML private TextField relayPortField;
    @FXML private Label statusLabel;
    @FXML private Button joinButton;

    @FXML
    public void initialize() {
        ToggleGroup group = new ToggleGroup();
        directRadio.setToggleGroup(group);
        relayRadio.setToggleGroup(group);
        directRadio.setSelected(true);

        relayRadio.selectedProperty().addListener((obs, old, selected) -> {
            relayBox.setVisible(selected);
            relayBox.setManaged(selected);
        });
    }

    @FXML
    private void handleJoin() {
        String myId = peerIdField.getText().trim();
        String portText = portField.getText().trim();

        if (myId.isEmpty()) {
            showError("Vui lòng nhập Peer ID!");
            return;
        }

        int myPort;
        try {
            myPort = Integer.parseInt(portText);
            if (myPort < 1024 || myPort > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Port phải là số từ 1024 đến 65535.");
            return;
        }

        String joinMode = relayRadio.isSelected() ? "2" : "1";
        String knownIp = relayIpField.getText().trim();
        int knownPort = 0;

        if ("2".equals(joinMode)) {
            if (knownIp.isEmpty()) { showError("Nhập IP peer trung gian."); return; }
            try {
                knownPort = Integer.parseInt(relayPortField.getText().trim());
            } catch (NumberFormatException e) {
                showError("Port relay không hợp lệ.");
                return;
            }
        }

        joinButton.setDisable(true);
        showInfo("Đang kết nối...");

        final int finalMyPort = myPort;
        final String finalJoinMode = joinMode;
        final String finalKnownIp = knownIp;
        final int finalKnownPort = knownPort;

        new Thread(() -> {
            try {
                PeerClient peerClient = new PeerClient();
                PeerMessagingAdapter messaging = new PeerMessagingAdapter(peerClient);
                Gson gson = new Gson();
                PeerSessionContext session = new PeerSessionContext(myId, finalMyPort, messaging, gson);
                PeerApplicationServices services = PeerApplicationServices.create(session);
                AtomicBoolean running = new AtomicBoolean(true);

                PeerServer server = new PeerServer(finalMyPort, myId, messaging);
                server.setDaemon(true);
                server.start();

                PeerInfo self = new PeerInfo(myId, AppConfig.peerAdvertiseHost(), finalMyPort);
                Message regMsg = new Message("REGISTER", myId, gson.toJson(self));

                PeerJoinService joinService = new PeerJoinService(session, services.offline);
                boolean ok = joinService.completeJoin(regMsg, finalJoinMode, finalKnownIp, finalKnownPort, running);

                if (ok) {
                    openChatScreen(myId, finalMyPort, session, services, running, server,
                            joinService.getPendingOfflineMessages());
                } else {
                    Platform.runLater(() -> {
                        showError("Không thể kết nối! Kiểm tra Tracker đang chạy hoặc IP/Port relay.");
                        joinButton.setDisable(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Lỗi: " + e.getMessage());
                    joinButton.setDisable(false);
                });
            }
        }).start();
    }

    private void openChatScreen(String myId, int myPort,
                                PeerSessionContext session, PeerApplicationServices services,
                                AtomicBoolean running, PeerServer server,
                                java.util.List<String> offlineMsgs) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("chat.fxml"));
                Scene scene = new Scene(loader.load(), 980, 640);
                scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

                // setScene trước để getWindow() trong init() không null
                Stage stage = (Stage) joinButton.getScene().getWindow();
                stage.setTitle("P2P Chat  —  " + myId + "  @  :" + myPort);
                stage.setResizable(true);
                stage.setScene(scene);
                stage.centerOnScreen();

                ChatController chatController = loader.getController();
                chatController.init(myId, myPort, session, services, running, server, offlineMsgs);
            } catch (Exception e) {
                showError("Lỗi mở cửa sổ chat: " + e.getMessage());
                joinButton.setDisable(false);
            }
        });
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().setAll("status-error");
    }

    private void showInfo(String msg) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().setAll("status-info");
    }
}
