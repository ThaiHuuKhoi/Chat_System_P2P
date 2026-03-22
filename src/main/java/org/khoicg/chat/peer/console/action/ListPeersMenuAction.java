package org.khoicg.chat.peer.console.action;

import org.khoicg.chat.model.PeerInfo;
import org.khoicg.chat.peer.console.MenuAction;
import org.khoicg.chat.peer.console.MenuResult;
import org.khoicg.chat.peer.service.PeerApplicationServices;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.List;
import java.util.Scanner;

public final class ListPeersMenuAction implements MenuAction {

    @Override
    public String id() {
        return "1";
    }

    @Override
    public String title() {
        return "Xem danh sách đang Online";
    }

    @Override
    public MenuResult execute(Scanner scanner, PeerSessionContext session, PeerApplicationServices services) {
        List<PeerInfo> peers = services.directory.fetchOnlinePeers();
        if (peers == null) {
            return MenuResult.CONTINUE;
        }
        System.out.println("\n--- DANH SÁCH ONLINE ---");
        for (PeerInfo p : peers) {
            System.out.println("- " + p.getPeerId() + " (IP: " + p.getIpAddress() + ", Port: " + p.getPort() + ")");
        }
        return MenuResult.CONTINUE;
    }
}
