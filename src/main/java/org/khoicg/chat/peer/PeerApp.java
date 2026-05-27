package org.khoicg.chat.peer;

import com.google.gson.Gson;
import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.service.FileTransferRegistry;

import java.util.Scanner;

/**
 * Entry point: wires {@link PeerMessagingAdapter}, {@link PeerServer}, {@link PeerConsoleApplication} (composition root).
 */
public final class PeerApp {

    private PeerApp() {}

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        PeerClient peerClient = new PeerClient();
        PeerMessagingAdapter messaging = new PeerMessagingAdapter(peerClient);
        Gson gson = new Gson();

        System.out.println("===============================");
        System.out.println("    CHÀO MỪNG ĐẾN P2P CHAT     ");
        System.out.println("===============================");

        System.out.print("Nhập Tên của bạn (Peer ID): ");
        String myId = scanner.nextLine();

        System.out.print("Nhập Port để nhận tin nhắn (vd: 5001, 5002...): ");
        int myPort = readPort(scanner);
        if (myPort < 0) {
            System.out.println("[-] Port không hợp lệ. Thoát.");
            System.exit(1);
        }

        PeerInfo self = new PeerInfo(myId, AppConfig.peerAdvertiseHost(), myPort);

        FileTransferRegistry fileRegistry = new FileTransferRegistry(AppConfig.fileTransferTimeoutMs());
        PeerServer server = new PeerServer(myPort, myId, messaging, fileRegistry);
        server.start();

        Message regMsg = new Message("REGISTER", myId, gson.toJson(self));

        System.out.print("\nTham gia mạng — (1) Tracker trực tiếp  (2) Qua peer đã biết (relay đăng ký): ");

        String joinMode = scanner.nextLine().trim();

        String knownIp = "";
        int knownPort = 0;
        if ("2".equals(joinMode)) {
            System.out.print("IP peer đã biết: ");
            knownIp = scanner.nextLine();
            System.out.print("Port peer đã biết: ");
            knownPort = readPort(scanner);
            if (knownPort < 0) {
                System.out.println("[-] Port không hợp lệ. Thoát.");
                System.exit(1);
            }
        }

        PeerConsoleApplication app = new PeerConsoleApplication(scanner, messaging, myId, myPort, fileRegistry);
        if (!app.completeJoin(regMsg, joinMode, knownIp, knownPort)) {
            System.exit(0);
        }

        app.runMainMenu();
    }

    private static int readPort(Scanner scanner) {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
