package org.khoicg.chat.peer.console.action;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.console.MenuAction;
import org.khoicg.chat.peer.console.MenuResult;
import org.khoicg.chat.peer.service.PeerApplicationServices;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ExitMenuAction implements MenuAction {

    private final AtomicBoolean running;

    public ExitMenuAction(AtomicBoolean running) {
        this.running = running;
    }

    @Override
    public String id() {
        return "7";
    }

    @Override
    public String title() {
        return "Thoát";
    }

    @Override
    public MenuResult execute(Scanner scanner, PeerSessionContext session, PeerApplicationServices services) {
        System.out.println("Đang thông báo cho Tracker và thoát hệ thống...");
        session.messaging().sendTracker(new Message("QUIT", session.myId(), ""));
        running.set(false);
        System.exit(0);
        return MenuResult.EXIT_APPLICATION;
    }
}
