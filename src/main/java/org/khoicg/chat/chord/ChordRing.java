package org.khoicg.chat.chord;

import org.khoicg.chat.model.PeerInfo;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Lớp phủ Chord rút gọn (theo Stoica et al. / Tanenbaum–Van Steen, mục DHT):
 * không gian định danh [0, 2^m), băm SHA-256, {@link #findSuccessor(BigInteger)},
 * bảng finger[i] = successor(n + 2^i mod 2^m).
 * <p>
 * Tracker giữ vòng; tin nhắn chat vẫn P2P trực tiếp — Chord dùng để minh họa <b>định vị khóa / peer</b> trong báo cáo.</p>
 */
public final class ChordRing {

    private final int m;
    private final BigInteger modulo;
    private final ConcurrentSkipListMap<BigInteger, PeerInfo> ring = new ConcurrentSkipListMap<>();

    public ChordRing(int identifierBits) {
        if (identifierBits < 4 || identifierBits > 16) {
            throw new IllegalArgumentException("chord.m nên trong [4, 16]");
        }
        this.m = identifierBits;
        this.modulo = BigInteger.ONE.shiftLeft(m);
    }

    public int getM() {
        return m;
    }

    public BigInteger getModulo() {
        return modulo;
    }

    /** Định danh Chord của peerId trong vòng 2^m (SHA-256 rồi mod). */
    public BigInteger chordId(String peerId) {
        return hashToRing(peerId);
    }

    /** Băm chuỗi bất kỳ (khóa dữ liệu) vào cùng không gian identifier. */
    public BigInteger keyHash(String key) {
        return hashToRing(key);
    }

    private BigInteger hashToRing(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(s.getBytes(StandardCharsets.UTF_8));
            BigInteger bi = new BigInteger(1, h);
            return bi.mod(modulo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addPeer(PeerInfo p) {
        BigInteger id = chordId(p.getPeerId());
        PeerInfo old = ring.put(id, p);
        if (old != null && !old.getPeerId().equals(p.getPeerId())) {
            System.err.println("[Chord] Cảnh báo: trùng hash identifier giữa " + old.getPeerId() + " và " + p.getPeerId());
        }
    }

    public void removePeer(String peerId) {
        if (peerId == null) return;
        BigInteger id = chordId(peerId);
        PeerInfo cur = ring.get(id);
        if (cur != null && cur.getPeerId().equals(peerId)) {
            ring.remove(id);
        }
    }

    /**
     * Successor(k): nút đầu tiên theo chiều kim đồng hồ từ k trong vòng (wrap nếu cần).
     */
    public PeerInfo findSuccessor(BigInteger k) {
        if (ring.isEmpty()) {
            return null;
        }
        k = k.mod(modulo);
        Map.Entry<BigInteger, PeerInfo> e = ring.ceilingEntry(k);
        if (e != null) {
            return e.getValue();
        }
        return ring.firstEntry().getValue();
    }

    public List<FingerRow> fingerTableFor(String peerId) {
        PeerInfo selfNode = ring.get(chordId(peerId));
        if (selfNode == null || !selfNode.getPeerId().equals(peerId)) {
            return List.of();
        }
        BigInteger n = chordId(peerId);
        List<FingerRow> rows = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            BigInteger start = n.add(BigInteger.ONE.shiftLeft(i)).mod(modulo);
            PeerInfo succ = findSuccessor(start);
            FingerRow fr = new FingerRow();
            fr.i = i;
            fr.intervalStartHex = start.toString(16);
            fr.successorPeerId = succ != null ? succ.getPeerId() : "";
            rows.add(fr);
        }
        return rows;
    }

    public RingView snapshot() {
        RingView v = new RingView();
        v.m = m;
        v.moduloHex = modulo.toString(16);
        v.nodes = new ArrayList<>();
        for (Map.Entry<BigInteger, PeerInfo> e : ring.entrySet()) {
            NodeEntry ne = new NodeEntry();
            ne.chordIdHex = e.getKey().toString(16);
            ne.peerId = e.getValue().getPeerId();
            v.nodes.add(ne);
        }
        return v;
    }

    public LookupView lookup(String key) {
        BigInteger kh = keyHash(key);
        PeerInfo succ = findSuccessor(kh);
        LookupView lv = new LookupView();
        lv.key = key;
        lv.keyHashHex = kh.toString(16);
        lv.successor = succ;
        return lv;
    }

    /** Dòng trong bảng finger (serializable Gson). */
    public static class FingerRow {
        public int i;
        public String intervalStartHex;
        public String successorPeerId;
    }

    public static class NodeEntry {
        public String chordIdHex;
        public String peerId;
    }

    public static class RingView {
        public int m;
        public String moduloHex;
        public List<NodeEntry> nodes;
    }

    public static class LookupView {
        public String key;
        public String keyHashHex;
        public PeerInfo successor;
    }

    public static class FingerView {
        public String peerId;
        public String chordIdHex;
        public int m;
        public List<FingerRow> fingers;
    }

    public FingerView fingerViewFor(String peerId) {
        FingerView fv = new FingerView();
        fv.peerId = peerId;
        fv.chordIdHex = chordId(peerId).toString(16);
        fv.m = m;
        fv.fingers = fingerTableFor(peerId);
        return fv;
    }
}
