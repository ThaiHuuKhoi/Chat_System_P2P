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
}
