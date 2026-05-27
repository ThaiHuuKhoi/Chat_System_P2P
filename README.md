# P2P Chat System (Chat_System_P2P)

**Chủ đề 3:** Hệ thống phân tán — chat ngang hàng (Java): tracker bootstrap, tin P2P TCP, nhóm/broadcast, relay, offline store, mã hóa AES, mô phỏng churn. (Chord/DHT đã gỡ khỏi mã nguồn.)

---

<details>
<!-- <summary>🎬 Hướng dẫn chạy (xem video demo)</summary> -->

<!-- 
  Cách thêm video:
  1. Mở file README.md trên GitHub web (nút Edit / bút chì)
  2. Kéo thả file .mp4 / .mov vào vùng soạn thảo — GitHub tự upload và trả về URL dạng:
     https://github.com/<user>/<repo>/assets/.../<tên-file>.mp4
  3. Thay URL bên dưới bằng URL vừa nhận được, rồi xoá dòng comment này.
-->

<video src="THAY_URL_VIDEO_Ở_ĐÂY.mp4" controls width="100%"></video>

</details>

---

## Yêu cầu

| Công cụ | Phiên bản tối thiểu |
|---|---|
| JDK | 21+ |
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

## Chạy nhanh bằng JAR (không cần clone / Maven)

Tải 2 file JAR từ thư mục `dist/` về cùng một thư mục, rồi mở **2 terminal**:

**Terminal 1 — Tracker**
```bash
java -jar tracker.jar
```

**Terminal 2, 3, … — Peer (giao diện đồ họa)**
```bash
java -jar peer.jar
```

> Yêu cầu: **JDK 21+** đã cài và có trong PATH. 

---

## Đóng gói JAR (dành cho dev)

```bash
mvn package -Pdist
```

Tạo ra trong `target/`:
- `tracker.jar` — tracker độc lập
- `peer.jar` — peer GUI, đã bundle JavaFX Windows

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
