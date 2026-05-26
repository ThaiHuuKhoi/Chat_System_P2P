package org.khoicg.chat.tracker;

import com.google.gson.Gson;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.model.PeerInfo;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gửi thông báo PEER_JOINED / PEER_LEFT tới các peer đang online bằng cách
 * mở TCP connection trực tiếp tới PeerServer của từng peer.
 * Mỗi lần push chạy trên thread riêng (non-blocking với handler).
 */
public final class TrackerPushBroadcaster {

    private static final int CONNECT_TIMEOUT_MS = 2_000;

    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });
    private final Gson gson = new Gson();

    /**
     * Broadcast msg tới tất cả targets, bỏ qua excludePeerId (null = gửi tất cả).
     */
    public void broadcast(Message msg, String excludePeerId, Collection<PeerInfo> targets) {
        String json = gson.toJson(msg);
        for (PeerInfo peer : targets) {
            if (excludePeerId != null && excludePeerId.equals(peer.getPeerId())) continue;
            String ip = peer.getIpAddress();
            int port = peer.getPort();
            pool.submit(() -> push(ip, port, json));
        }
    }

    private void push(String ip, int port, String json) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(json);
        } catch (Exception ignored) {
            // peer không thể reach được — bỏ qua
        }
    }

    public void shutdown() {
        pool.shutdown();
    }
}
