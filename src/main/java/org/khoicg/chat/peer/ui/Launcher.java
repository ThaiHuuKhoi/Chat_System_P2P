package org.khoicg.chat.peer.ui;

/**
 * Launcher không extend Application — bypass kiểm tra JavaFX module khi chạy từ classpath.
 * Dùng class này làm main class trong IntelliJ thay vì PeerFxApp.
 */
public class Launcher {
    public static void main(String[] args) {
        PeerFxApp.main(args);
    }
}
