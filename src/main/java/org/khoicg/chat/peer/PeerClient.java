package org.khoicg.chat.peer;

import com.google.gson.Gson;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.util.ReliableDeliveryHelper;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class PeerClient {
    private static final int MAX_RELIABLE_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 400;

    private final Gson gson = new Gson();

    public String sendRequest(String targetIp, int targetPort, Message message) {
        try {
            return executeSend(targetIp, targetPort, message);
        } catch (SocketTimeoutException e) {
            System.out.println("[-] Lỗi: " + targetIp + ":" + targetPort + " không phản hồi (Timeout).");
            return null;
        } catch (IOException e) {
            System.out.println("[-] Lỗi: Không thể kết nối tới " + targetIp + ":" + targetPort + " (Peer Offline).");
            return null;
        }
    }

    public String sendReliableRequest(String targetIp, int targetPort, Message message) {
        String expectedId = message.getMessageId();
        String last = null;
        for (int attempt = 0; attempt < MAX_RELIABLE_ATTEMPTS; attempt++) {
            if (attempt > 0) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            try {
                last = executeSend(targetIp, targetPort, message);
            } catch (IOException e) {
                last = null;
            }
            if (ReliableDeliveryHelper.ackJsonMatches(last, expectedId)) {
                return last;
            }
        }
        System.out.println("[-] Sau " + MAX_RELIABLE_ATTEMPTS + " lần thử: không nhận ACK hợp lệ từ "
                + targetIp + ":" + targetPort);
        return null;
    }

    private String executeSend(String targetIp, int targetPort, Message message) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(targetIp, targetPort), 3000);
            socket.setSoTimeout(3000);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(gson.toJson(message));
            return in.readLine();
        }
    }
}
