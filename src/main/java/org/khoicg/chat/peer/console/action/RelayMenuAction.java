package org.khoicg.chat.peer.console.action;

import org.khoicg.chat.peer.console.MenuAction;
import org.khoicg.chat.peer.console.MenuResult;
import org.khoicg.chat.peer.service.PeerApplicationServices;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.Scanner;

public final class RelayMenuAction implements MenuAction {

    @Override
    public String id() {
        return "5";
    }

    @Override
    public String title() {
        return "Gửi tin qua peer trung gian (relay)";
    }

    @Override
    public MenuResult execute(Scanner scanner, PeerSessionContext session, PeerApplicationServices services) {
        services.relaySend.runInteractive(scanner);
        return MenuResult.CONTINUE;
    }
}
