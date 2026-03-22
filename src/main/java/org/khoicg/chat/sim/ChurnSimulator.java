package org.khoicg.chat.sim;

import com.google.gson.Gson;
import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.PeerClient;
import org.khoicg.chat.peer.PeerServer;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mô phỏng churn: peer ảo lặp REGISTER → HEARTBEAT → QUIT.
 *
 * <p>Chạy sau khi {@code TrackerServer} đã bật.</p>
 */
public final class ChurnSimulator {

    private static final Gson GSON = new Gson();

    private ChurnSimulator() {}

    public static void main(String[] args) {
        int numPeers = argInt(args, 0, 5);
        int durationSec = argInt(args, 1, 60);
        int basePort = argInt(args, 2, 6100);
        String trackerHost = args.length > 3 ? args[3].trim() : AppConfig.trackerHost();
        int trackerPort = args.length > 4 ? Integer.parseInt(args[4].trim()) : AppConfig.trackerPort();

        System.out.println("=== Churn Simulator ===");
        System.out.println("Tracker: " + trackerHost + ":" + trackerPort);
        System.out.println("Số peer ảo: " + numPeers + ", thời gian: " + durationSec + "s, port từ " + basePort);
        System.out.println("Ctrl+C để dừng sớm.\n");

        AtomicBoolean running = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            System.out.println("\n[Churn] Đang dừng...");
        }));

        Thread[] workers = new Thread[numPeers];
        for (int i = 0; i < numPeers; i++) {
            int port = basePort + i;
            String peerId = "churn_" + i;
            workers[i] = new Thread(() -> churnLoop(peerId, port, trackerHost, trackerPort, running), "churn-" + peerId);
            workers[i].start();
        }

        try {
            Thread.sleep(durationSec * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        running.set(false);
        for (Thread t : workers) {
            try {
                t.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[Churn] Kết thúc mô phỏng.");
    }

    private static int argInt(String[] args, int idx, int def) {
        if (args.length <= idx) return def;
        try {
            return Integer.parseInt(args[idx].trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void churnLoop(String peerId, int listenPort, String trackerHost, int trackerPort, AtomicBoolean globalRun) {
        PeerClient client = new PeerClient();
        PeerServer server = new PeerServer(listenPort, peerId);
        server.setDaemon(true);
        server.start();

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        while (globalRun.get()) {
            PeerInfo info = new PeerInfo(peerId, AppConfig.peerAdvertiseHost(), listenPort);
            Message reg = new Message("REGISTER", peerId, GSON.toJson(info));
            String regResp = client.sendRequest(trackerHost, trackerPort, reg);
            if (regResp == null) {
                System.out.println("[Churn] " + peerId + " REGISTER thất bại — thử lại sau 2s");
                sleepQuiet(2000);
                continue;
            }
            System.out.println("[Churn] " + peerId + " → ONLINE (port " + listenPort + ")");

            long onlineMs = ThreadLocalRandom.current().nextLong(3000L, 15000L);
            long deadline = System.currentTimeMillis() + onlineMs;

            while (globalRun.get() && System.currentTimeMillis() < deadline) {
                sleepQuiet(5000);
                if (!globalRun.get()) break;
                client.sendRequest(trackerHost, trackerPort, new Message("HEARTBEAT", peerId, "ping"));
            }

            if (!globalRun.get()) break;

            client.sendRequest(trackerHost, trackerPort, new Message("QUIT", peerId, ""));
            System.out.println("[Churn] " + peerId + " → OFFLINE");

            long offlineMs = ThreadLocalRandom.current().nextLong(800L, 4000L);
            sleepQuiet(offlineMs);
        }
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
