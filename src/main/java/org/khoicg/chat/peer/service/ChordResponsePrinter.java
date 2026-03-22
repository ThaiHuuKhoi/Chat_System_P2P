package org.khoicg.chat.peer.service;

import com.google.gson.Gson;
import org.khoicg.chat.chord.ChordRing;
import org.khoicg.chat.model.Message;

/**
 * Renders CHORD_DATA JSON to stdout (SRP: view only).
 */
public final class ChordResponsePrinter {

    private final Gson gson;

    public ChordResponsePrinter(Gson gson) {
        this.gson = gson;
    }

    public void print(String jsonLine, int mode) {
        if (jsonLine == null) {
            System.out.println("[-] Không nhận được dữ liệu từ Tracker.");
            return;
        }
        try {
            Message resp = gson.fromJson(jsonLine, Message.class);
            if (!"CHORD_DATA".equals(resp.getType()) || resp.getContent() == null) {
                System.out.println("[-] Phản hồi không phải CHORD_DATA: " + jsonLine);
                return;
            }
            String c = resp.getContent();
            if (mode == 1) {
                printRing(c);
            } else if (mode == 2) {
                printFinger(c);
            } else if (mode == 3) {
                printLookup(c);
            }
        } catch (Exception e) {
            System.out.println("[-] Lỗi khi đọc CHORD_DATA: " + e.getMessage());
        }
    }

    private void printRing(String c) {
        ChordRing.RingView rv = gson.fromJson(c, ChordRing.RingView.class);
        System.out.println("m = " + rv.m + ", modulo (hex) = " + rv.moduloHex);
        System.out.println("--- Các nút trên vòng (theo thứ tự identifier) ---");
        if (rv.nodes == null || rv.nodes.isEmpty()) {
            System.out.println("(trống)");
        } else {
            for (ChordRing.NodeEntry n : rv.nodes) {
                System.out.println("  id(hex)=" + n.chordIdHex + "  ->  peer=" + n.peerId);
            }
        }
    }

    private void printFinger(String c) {
        ChordRing.FingerView fv = gson.fromJson(c, ChordRing.FingerView.class);
        System.out.println("Peer: " + fv.peerId + " | chordId(hex)=" + fv.chordIdHex + " | m=" + fv.m);
        if (fv.fingers == null || fv.fingers.isEmpty()) {
            System.out.println("(không có finger — peer không có trên vòng Chord)");
        } else {
            for (ChordRing.FingerRow row : fv.fingers) {
                System.out.println("  finger[" + row.i + "] start(hex)=" + row.intervalStartHex
                        + "  ->  successor=" + row.successorPeerId);
            }
        }
    }

    private void printLookup(String c) {
        ChordRing.LookupView lv = gson.fromJson(c, ChordRing.LookupView.class);
        System.out.println("Khóa: " + lv.key);
        System.out.println("Hash khóa (hex, trong vòng): " + lv.keyHashHex);
        if (lv.successor == null) {
            System.out.println("Successor: (không có — vòng trống)");
        } else {
            System.out.println("Successor (peer chịu trách nhiệm theo Chord): "
                    + lv.successor.getPeerId() + " @ " + lv.successor.getIpAddress() + ":" + lv.successor.getPort());
        }
    }
}
