# Áp dụng giáo trình *Distributed Systems* (Tanenbaum & Van Steen, 3rd ed.) vào dự án P2P Chat

Tài liệu gốc: `Distributed_Systems_3.pdf` (bản 3.03, 2020).  
Mục đích: nối **khái niệm lý thuyết** với **class / hành vi** trong mã nguồn để viết báo cáo và bảo vệ.

---

## 1. Chương 1 — Introduction (hệ thống phân tán, mục tiêu, pitfalls)

| Khái niệm | Ý trong giáo trình | Trong dự án |
|-----------|-------------------|---------------|
| **Resource sharing** | Nhiều máy dùng chung tài nguyên / dịch vụ | Peer “dùng chung” **danh sách peer** và (tuỳ chọn) **hàng đợi offline** qua tracker |
| **Scalability** | Hệ thống chịu tải khi số nút tăng | Tracker là điểm tập trung — trong phạm vi đồ án **chấp nhận được**; báo cáo nên **thừa nhận giới hạn** khi số peer rất lớn |
| **Pitfall: mạng luôn tin cậy / không trễ** | Không nên giả định mạng hoàn hảo | **Timeout**, **retry ACK**, **STORE_OFFLINE**, **heartbeat + timeout gỡ peer** |

**Câu gợi ý cho báo cáo:** *“Chúng em tránh pitfall ‘mạng luôn ổn định’ bằng timeout kết nối/đọc, cơ chế ACK + messageId, và lưu tin trên tracker khi P2P thất bại.”*

---

## 2. Chương 2 — Architectures (client–server, P2P, hybrid)

| Khái niệm | Trong dự án |
|-----------|-------------|
| **Centralized** | **TrackerServer**: một dịch vụ đăng ký / danh bạ / offline |
| **Decentralized P2P** | **PeerClient → PeerServer** trực tiếp cho `CHAT`, `FILE`, … |
| **Hybrid** | **Đúng mô hình đồ án:** bootstrap tập trung + truyền tin ngang hàng — tương tự các hệ thống P2P thực tế có **tracker** hoặc **superpeer** |

**Class liên quan:** `TrackerServer`, `PeerApp`, `PeerServer`, `PeerClient`.

---

## 3. Chương 3 — Processes (tiến trình, server đa luồng)

| Khái niệm | Trong dự án |
|-----------|-------------|
| **Concurrent requests** | `TrackerHandler extends Thread`; mỗi kết nối tracker một luồng |
| **Concurrent incoming messages** | `PeerServer`: mỗi `accept()` spawn thread xử lý `handleIncomingMessage` |

**Ý báo cáo:** peer vừa **client** vừa **server** — mỗi vai trò có luồng riêng, không chặn menu chính.

---

## 4. Chương 4 — Communication (giao tiếp, socket, thông điệp)

| Khái niệm | Trong dự án |
|-----------|-------------|
| **TCP / stream** | `Socket`, `ServerSocket` — luồng byte tin cậy theo kết nối |
| **Message-oriented trên nền TCP** | Mỗi “thông điệp ứng dụng” = **một dòng** JSON (`println` / `readLine`) — dễ parse, phù hợp lab |
| **Reliability ở tầng ứng dụng** | ACK + `messageId` + retry — bổ sung cho việc “đã xử lý xong hay chưa” mà TCP không biết |

**Class:** `PeerClient`, `PeerServer`, `ReliableDeliveryHelper`.

---

## 5. Chương 5 — Naming (tên, địa chỉ, tra cứu)

| Khái niệm | Trong dự án |
|-----------|-------------|
| **Human-friendly name** | `peerId` (chuỗi do người dùng nhập) |
| **Address for communication** | `PeerInfo`: `ipAddress` + `port` |
| **Name resolution** | `GET_PEERS` trả về danh sách — peer gửi **dùng IP:port** để mở TCP |

**Ý báo cáo:** discovery qua tracker là dạng **dịch vụ tra cứu đơn giản** (không phải DNS hay DHT như sách mô tả nâng cao).

---

## 6. Chord DHT (Ch.5 — bảng băm phân tán; Stoica et al., SIGCOMM 2001; DS3 mục DHT)

| Khái niệm (sách / bài Chord gốc) | Trong dự án |
|----------------------------------|-------------|
| **Không gian identifier** hình vòng modulo \(2^m\) | `ChordRing`: `m` cấu hình `chord.m` (mặc định **8** bit → 256 điểm), `modulo = 2^m` |
| **Ánh xạ nút / khóa** vào vòng | **SHA-256** rồi `mod 2^m` — cùng ý **consistent hashing** |
| **Successor(k)** | `findSuccessor`: nút đầu tiên theo chiều kim đồng hồ từ `k`, **bọc vòng** nếu cần |
| **Finger table** | `finger[i] = successor((n + 2^i) mod 2^m)` — đúng định nghĩa giáo trình |
| **Triển khai phân tán đầy đủ** (stabilize, fix_fingers, giao tiếp peer–peer trong vòng) | **Chưa** — để giảm độ phức tạp đồ án, **vòng Chord và finger được tính tập trung trên Tracker**; peer vẫn **tra cứu qua `CHORD_*`** để **minh họa lý thuyết** |

**Mã nguồn:** `org.khoicg.chat.chord.ChordRing`, tích hợp `TrackerServer` (`CHORD_RING`, `CHORD_FINGER`, `CHORD_LOOKUP`); menu **6** trong `PeerApp`.

**Đoạn viết báo cáo gợi ý:** *“Nhóm triển khai cấu trúc dữ liệu và thuật toán **successor / finger** theo Chord trên tracker như một **mô hình tham chiếu**; luồng chat vẫn **P2P trực tiếp**. Việc đặt trạng thái vòng tại tracker tương tự kiến trúc **hybrid** / siêu nút hỗ trợ DHT trong thực tế, phù hợp phạm vi môn học.”*

**Trích dẫn thêm (tùy khoa):** Stoica, I. et al. (2001). *Chord: A scalable peer-to-peer lookup service for internet applications.* ACM SIGCOMM.

---

## 7. Multicast vs broadcast nhóm (Ch.4 — khái niệm)

Giáo trình có **application-level multicast** (cây, flooding, gossip).  
**Dự án:** broadcast / nhóm = **gửi lặp unicast** tới từng `PeerInfo` — đúng yêu cầu “phát tới cả nhóm”, đơn giản, dễ chứng minh; có thể nêu **không dùng IP multicast** vì phạm vi đồ án / mạng LAN.

---

## 8. Gợi ý trích dẫn (Harvard / IEEE tùy khoa)

```
Tanenbaum, A.S. & Van Steen, M. (2020) Distributed Systems, 3rd ed. v3.03.
```

Khi trích **khái niệm cụ thể**, thêm *“Chương X, mục …”* theo mục lục sách.

---

## 9. Checklist trước khi nộp

- [ ] Trong **Kiến trúc**, có ít nhất một câu gọi tên **hybrid** (Ch.2).  
- [ ] Trong **Xử lý lỗi**, liên hệ **pitfall mạng** (Ch.1).  
- [ ] Trong **Discovery**, liên hệ **tên vs địa chỉ** (Ch.5).  
- [ ] Trong **Giao tiếp**, nêu **TCP + thông điệp ứng dụng JSON** (Ch.4).  
- [ ] Mục **Chord / DHT**: successor, finger, không gian \(2^m\), và **giới hạn** (vòng trên tracker).  
- [ ] **Tài liệu tham khảo** có đủ bản ghi sách DS3 (+ tùy chọn bài Chord gốc).
