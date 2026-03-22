package org.khoicg.chat.peer.console;

import org.khoicg.chat.peer.service.PeerApplicationServices;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public final class MainMenuLoop {

    private final Scanner scanner;
    private final PeerSessionContext session;
    private final PeerApplicationServices services;
    private final Map<String, MenuAction> actionsById;

    public MainMenuLoop(Scanner scanner, PeerSessionContext session, PeerApplicationServices services,
                        List<MenuAction> actionsInOrder) {
        this.scanner = scanner;
        this.session = session;
        this.services = services;
        Map<String, MenuAction> map = new LinkedHashMap<>();
        for (MenuAction a : actionsInOrder) {
            map.put(a.id(), a);
        }
        this.actionsById = map;
    }

    public void runForever() {
        while (true) {
            System.out.println("\n--- MENU ---");
            for (MenuAction a : actionsById.values()) {
                System.out.println(a.id() + ". " + a.title());
            }
            System.out.print("Chọn chức năng: ");
            String choice = scanner.nextLine();
            MenuAction action = actionsById.get(choice.trim());
            if (action == null) {
                System.out.println("Lựa chọn không hợp lệ!");
                continue;
            }
            MenuResult r = action.execute(scanner, session, services);
            if (r == MenuResult.EXIT_APPLICATION) {
                return;
            }
        }
    }
}
