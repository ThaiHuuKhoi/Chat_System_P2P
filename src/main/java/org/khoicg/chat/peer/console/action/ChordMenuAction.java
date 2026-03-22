package org.khoicg.chat.peer.console.action;

import org.khoicg.chat.peer.console.MenuAction;
import org.khoicg.chat.peer.console.MenuResult;
import org.khoicg.chat.peer.service.PeerApplicationServices;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.Scanner;

public final class ChordMenuAction implements MenuAction {

    @Override
    public String id() {
        return "6";
    }

    @Override
    public String title() {
        return "Chord DHT — vòng identifier / finger / tra successor (giáo trình)";
    }

    @Override
    public MenuResult execute(Scanner scanner, PeerSessionContext session, PeerApplicationServices services) {
        services.chord.runInteractive(scanner);
        return MenuResult.CONTINUE;
    }
}
