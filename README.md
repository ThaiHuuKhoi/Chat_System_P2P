# P2P Chat System (Chat_System_P2P)

**Chủ đề 3:** Hệ thống phân tán — chat ngang hàng (Java): tracker bootstrap, tin P2P TCP, nhóm/broadcast, relay, offline store, mã hóa AES, mô phỏng churn. (Chord/DHT đã gỡ khỏi mã nguồn.)

## Yêu cầu

- JDK 24+
- Maven 3.9+

## Cấu hình

Chỉnh `src/main/resources/application.properties`:

- `tracker.host`, `tracker.port` — địa chỉ tracker
- `peer.advertise.host` — IP/hostname peer khai báo khi đăng ký (đổi khi chạy nhiều máy LAN)

## Giao diện web (Next.js)

Thư mục `web/`: **ứng dụng cho người dùng** — đăng ký tracker, heartbeat, danh sách peer, gửi/nhận tin (AES giống Java). API server mở TCP tới tracker và tới peer Java khi gửi tin.

```bash
cd web
# Tạo .env từ .env.example (PowerShell: Copy-Item .env.example .env)
npm install
npm run dev
```

Mở [http://localhost:3000](http://localhost:3000). Cần **TrackerServer** (`tracker.port` mặc định **9000**). Tùy chỉnh `TRACKER_*`, `WEB_PEER_ADVERTISE_HOST`, `WEB_DUMMY_LISTEN_PORT` trong `web/.env`.

**Ghi chú:** peer web đăng ký cổng “ảo” — tin tới thường qua **STORE_OFFLINE + PULL**; gửi tới peer Java **online** vẫn thử **TCP trực tiếp** trước (giống ứng dụng console).

## Chạy nhanh

**1. Tracker (terminal 1)**

```bash
mvn -q compile exec:java -Dexec.mainClass=org.khoicg.chat.tracker.TrackerServer
```

**2. Peer (terminal 2+)**

```bash
mvn -q compile exec:java
```

(hoặc `-Dexec.mainClass=org.khoicg.chat.peer.PeerApp`)

**3. Mô phỏng churn (tùy chọn)**

```bash
mvn -q compile exec:java -Dexec.mainClass=org.khoicg.chat.sim.ChurnSimulator -Dexec.args="5 60 6100"
```

Tham số: `[số_peer] [giây] [port_bắt_đầu] [tracker_host] [tracker_port]` — bỏ qua thì dùng mặc định / `application.properties`.

## Cấu trúc mã nguồn

```
src/main/java/org/khoicg/chat/
  config/       AppConfig — đọc application.properties
  model/        Message, PeerInfo
  util/         AES, messageId, ACK hợp lệ
  peer/         PeerApp, PeerClient, PeerServer
  tracker/      TrackerServer
  sim/          ChurnSimulator
src/main/resources/
  application.properties
```

## Tài liệu (báo cáo & lý thuyết)

- `docs/BAO_CAO_DO_AN.md` — khung báo cáo đồ án  
- `docs/AP_DUNG_GIAO_TRINH_DS.md` — **ánh xạ giáo trình** *Distributed Systems* (Tanenbaum & Van Steen) với kiến trúc và mã nguồn dự án  

## Gói nộp

Mã nguồn + báo cáo (kiến trúc, giao thức, discovery, xử lý lỗi & thử nghiệm) theo yêu cầu đồ án.
