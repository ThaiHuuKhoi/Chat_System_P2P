package org.khoicg.chat.peer.console;

import org.khoicg.chat.peer.service.PeerApplicationServices;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.Scanner;

/**
 * One main-menu item (OCP: add new action by new class + register in {@link MainMenuLoop}).
 */
public interface MenuAction {

    String id();

    String title();

    MenuResult execute(Scanner scanner, PeerSessionContext session, PeerApplicationServices services);
}
