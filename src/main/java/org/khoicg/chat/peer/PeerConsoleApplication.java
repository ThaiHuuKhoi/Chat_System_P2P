package org.khoicg.chat.peer;

import com.google.gson.Gson;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.net.MessagingClient;
import org.khoicg.chat.peer.console.MainMenuLoop;
import org.khoicg.chat.peer.console.MenuAction;
import org.khoicg.chat.peer.console.action.ChordMenuAction;
import org.khoicg.chat.peer.console.action.DirectChatMenuAction;
import org.khoicg.chat.peer.console.action.ExitMenuAction;
import org.khoicg.chat.peer.console.action.FileSendMenuAction;
import org.khoicg.chat.peer.console.action.GroupChatMenuAction;
import org.khoicg.chat.peer.console.action.ListPeersMenuAction;
import org.khoicg.chat.peer.console.action.RelayMenuAction;
import org.khoicg.chat.peer.service.PeerApplicationServices;
import org.khoicg.chat.peer.service.PeerJoinService;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wires session, join flow, and {@link MainMenuLoop} (composition — keeps {@link PeerApp} thin).
 */
public final class PeerConsoleApplication {

    private final Scanner scanner;
    private final PeerSessionContext session;
    private final PeerApplicationServices services;
    private final PeerJoinService joinService;
    private final AtomicBoolean running;

    public PeerConsoleApplication(Scanner scanner, MessagingClient messaging, String myId, int myPort) {
        this.scanner = scanner;
        Gson gson = new Gson();
        this.session = new PeerSessionContext(myId, myPort, messaging, gson);
        this.services = PeerApplicationServices.create(session);
        this.joinService = new PeerJoinService(session, services.offline);
        this.running = new AtomicBoolean(true);
    }

    /**
     * @param joinMode "1" = tracker trực tiếp, "2" = relay qua peer đã biết
     */
    public boolean completeJoin(Message registerMessage, String joinMode, String knownIp, int knownPort) {
        return joinService.completeJoin(registerMessage, joinMode, knownIp, knownPort, running);
    }

    public void runMainMenu() {
        MainMenuLoop loop = new MainMenuLoop(scanner, session, services, defaultMenuActions());
        loop.runForever();
    }

    private List<MenuAction> defaultMenuActions() {
        return List.of(
                new ListPeersMenuAction(),
                new DirectChatMenuAction(),
                new GroupChatMenuAction(),
                new FileSendMenuAction(),
                new RelayMenuAction(),
                new ChordMenuAction(),
                new ExitMenuAction(running)
        );
    }
}
