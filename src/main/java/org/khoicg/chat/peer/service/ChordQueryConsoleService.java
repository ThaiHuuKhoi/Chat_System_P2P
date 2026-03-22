package org.khoicg.chat.peer.service;

import org.khoicg.chat.model.Message;
import org.khoicg.chat.peer.session.PeerSessionContext;

import java.util.Scanner;

public final class ChordQueryConsoleService {

    private final PeerSessionContext session;
    private final ChordResponsePrinter printer;

    public ChordQueryConsoleService(PeerSessionContext session, ChordResponsePrinter printer) {
        this.session = session;
        this.printer = printer;
    }

    public void runInteractive(Scanner scanner) {
        System.out.println("--- Chord (DHT) — không gian 2^m trên Tracker ---");
        System.out.println(" (1) Xem vòng identifier + các nút");
        System.out.println(" (2) Bảng finger của tôi (" + session.myId() + ")");
        System.out.println(" (3) Tra successor cho một khóa (chuỗi bất kỳ)");
        System.out.print("Chọn: ");
        String chordOpt = scanner.nextLine().trim();

        if ("1".equals(chordOpt)) {
            String r = session.messaging().sendTracker(new Message("CHORD_RING", session.myId(), ""));
            printer.print(r, 1);
        } else if ("2".equals(chordOpt)) {
            String r = session.messaging().sendTracker(new Message("CHORD_FINGER", session.myId(), session.myId()));
            printer.print(r, 2);
        } else if ("3".equals(chordOpt)) {
            System.out.print("Nhập khóa (ví dụ tên file, tên phòng, chuỗi bất kỳ): ");
            String key = scanner.nextLine();
            String r = session.messaging().sendTracker(new Message("CHORD_LOOKUP", session.myId(), key));
            printer.print(r, 3);
        } else {
            System.out.println("[-] Lựa chọn không hợp lệ.");
        }
    }
}
