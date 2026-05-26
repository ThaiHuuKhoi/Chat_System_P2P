# P2P Chat System (Chat_System_P2P)

**Chủ đề 3:** Hệ thống phân tán — chat ngang hàng (Java): tracker bootstrap, tin P2P TCP, nhóm/broadcast, relay, offline store, mã hóa AES, mô phỏng churn. (Chord/DHT đã gỡ khỏi mã nguồn.)

---

## Yêu cầu

| Công cụ | Phiên bản tối thiểu |
|---|---|
| JDK | 24+ |
| Maven | 3.9+ |
| JavaFX | đã khai báo trong `pom.xml` (tự tải qua Maven) |

---

## Cấu hình

Chỉnh `src/main/resources/application.properties` trước khi chạy:

```properties
tracker.host=localhost      # IP/hostname máy chạy tracker
tracker.port=9000           # cổng tracker
peer.advertise.host=localhost  # IP peer tự khai báo khi đăng ký
                               # (đổi thành IP LAN khi chạy nhiều máy)
```

---

## Chạy nhanh (cùng máy — nhiều terminal)

### Bước 1 — Khởi động Tracker

```bash
mvn -q compile exec:java -Dexec.mainClass=org.khoicg.chat.tracker.TrackerServer
```

Tracker lắng nghe trên cổng `tracker.port` (mặc định **9000**).

---

### Bước 2 — Chạy Peer (chọn một trong hai chế độ)

#### Giao diện đồ họa (JavaFX) — khuyến nghị

```bash
mvn javafx:run
```

Cửa sổ đăng nhập hiện ra — nhập **tên hiển thị** và **cổng peer** rồi nhấn **Connect**.

#### Console (không có GUI)

```bash
mvn -q compile exec:java
# hoặc chỉ định rõ class:
mvn -q compile exec:java -Dexec.mainClass=org.khoicg.chat.peer.PeerApp
```

> Mở thêm terminal và lặp lại Bước 2 (với cổng khác) để có nhiều peer trên cùng máy.

---

## Chạy nhiều máy trên LAN

1. Đặt `tracker.host` = IP máy chạy tracker, `tracker.port` = cổng không bị firewall chặn.
2. Trên **mỗi peer**: đặt `peer.advertise.host` = IP LAN của máy đó (để các peer khác kết nối ngược lại được).
3. Khởi động tracker trước, rồi khởi động từng peer.

---

## Đóng gói JAR (Tracker)

```bash
mvn package
```

Tạo ra `target/tracker.jar` — chạy tracker độc lập (không cần Maven):

```bash
java -jar target/tracker.jar
```

---

## Cấu trúc mã nguồn

```
src/main/java/org/khoicg/chat/
  config/       AppConfig — đọc application.properties
  model/        Message, PeerInfo
  util/         AES, messageId, ACK hợp lệ
  peer/
    ui/         PeerFxApp, Launcher, LoginController, ChatController
                (giao diện JavaFX — login.fxml, chat.fxml)
    handler/    xử lý tin nhắn đến (chat, file, peer-changed, ...)
    service/    DirectChat, GroupChat, Relay, FileSend, Heartbeat, ...
    console/    menu console (dùng khi chạy không có GUI)
  tracker/
    handler/    xử lý lệnh từ peer (register, quit, store-offline, ...)
    TrackerServer, TrackerState, TrackerPushBroadcaster, ...

src/main/resources/
  application.properties
  org/khoicg/chat/peer/ui/   login.fxml, chat.fxml, style.css
```
