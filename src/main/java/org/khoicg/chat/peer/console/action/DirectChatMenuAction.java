package org.khoicg.chat.peer.console.action;

import org.khoicg.chat.peer.console.MenuAction;
import org.khoicg.chat.peer.console.MenuResult;
import org.khoicg.chat.peer.service.PeerApplicationServices;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.Scanner;

public final class DirectChatMenuAction implements MenuAction {

    @Override
    public String id() {
        return "2";
    }

    @Override
    public String title() {
        return "Nhắn tin trực tiếp (P2P)";
    }

    @Override
    public MenuResult execute(Scanner scanner, PeerSessionContext session, PeerApplicationServices services) {
        services.directChat.runInteractive(scanner);
        return MenuResult.CONTINUE;
    }
}
