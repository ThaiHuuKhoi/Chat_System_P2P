package org.khoicg.chat.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Cấu hình từ {@code classpath:application.properties} (có thể override bằng file cùng tên trong working directory nếu mở rộng sau).
 */
public final class AppConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException ignored) {
            // mặc định bên dưới
        }
    }

    private AppConfig() {}

    public static String trackerHost() {
        return PROPS.getProperty("tracker.host", "localhost");
    }

    public static int trackerPort() {
        return Integer.parseInt(PROPS.getProperty("tracker.port", "9000"));
    }

    /** IP/hostname peer khai báo khi REGISTER (peer khác dùng để kết nối P2P). */
    public static String peerAdvertiseHost() {
        return PROPS.getProperty("peer.advertise.host", "localhost");
    }

    public static int chunkSizeBytes() {
        return Integer.parseInt(PROPS.getProperty("file.chunk.size.bytes", "524288"));
    }

    public static long fileTransferTimeoutMs() {
        return Long.parseLong(PROPS.getProperty("file.transfer.timeout.ms", "86400000"));
    }

    // ─── Peer server ─────────────────────────────────────────────────────────────

    public static int peerServerThreadPoolSize() {
        return Integer.parseInt(PROPS.getProperty("peer.server.thread.pool.size", "20"));
    }

    public static int peerServerMaxMessageBytes() {
        return Integer.parseInt(PROPS.getProperty("peer.server.max.message.bytes", "67108864"));
    }

    public static int peerServerReadTimeoutMs() {
        return Integer.parseInt(PROPS.getProperty("peer.server.read.timeout.ms", "10000"));
    }

    public static long peerDirectoryCacheTtlMs() {
        return Long.parseLong(PROPS.getProperty("peer.directory.cache.ttl.ms", "5000"));
    }

    public static long peerFileMaxBytes() {
        return Long.parseLong(PROPS.getProperty("peer.file.send.max.bytes", "52428800"));
    }

    // ─── Tracker ─────────────────────────────────────────────────────────────────

    public static int trackerServerThreadPoolSize() {
        return Integer.parseInt(PROPS.getProperty("tracker.server.thread.pool.size", "10"));
    }

    public static int trackerOfflineQueueLimit() {
        return Integer.parseInt(PROPS.getProperty("tracker.offline.queue.limit", "50"));
    }

    public static long trackerStalePeerCheckIntervalMs() {
        return Long.parseLong(PROPS.getProperty("tracker.stale.peer.check.interval.ms", "10000"));
    }

    public static long trackerPeerTimeoutMs() {
        return Long.parseLong(PROPS.getProperty("tracker.peer.timeout.ms", "15000"));
    }

    public static int trackerPushConnectTimeoutMs() {
        return Integer.parseInt(PROPS.getProperty("tracker.push.connect.timeout.ms", "2000"));
    }

    public static int trackerClientMaxMessageBytes() {
        return Integer.parseInt(PROPS.getProperty("tracker.client.max.message.bytes", "1048576"));
    }

    public static int trackerClientReadTimeoutMs() {
        return Integer.parseInt(PROPS.getProperty("tracker.client.read.timeout.ms", "10000"));
    }
}
