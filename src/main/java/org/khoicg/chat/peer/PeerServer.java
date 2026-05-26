package org.khoicg.chat.peer;

import com.google.gson.Gson;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.net.MessagingClient;
import org.khoicg.chat.peer.handler.ChatInboundHandler;
import org.khoicg.chat.peer.handler.FileInboundHandler;
import org.khoicg.chat.peer.handler.PeerChangedInboundHandler;
import org.khoicg.chat.peer.handler.RelayInboundHandler;
import org.khoicg.chat.peer.handler.RelayRegisterInboundHandler;
import org.khoicg.chat.peer.ui.IncomingMessageListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerServer extends Thread {

    private static final int THREAD_POOL_SIZE = 20;
    private static final int MAX_MSG_BYTES    = 64 * 1024 * 1024; // 64 MB — đủ cho file 50 MB sau Base64
    private static final int READ_TIMEOUT_MS  = 10_000;

    private final int myPort;
    private final String myPeerId;
    private final Gson gson = new Gson();
    private final MessagingClient messaging;
    private final DeliveryIdempotencyTracker deduper = new DeliveryIdempotencyTracker();
    private final PeerMessageDispatcher dispatcher;
    private final ExecutorService requestPool;
    private volatile IncomingMessageListener messageListener;

    public PeerServer(int port, String myPeerId, MessagingClient messaging) {
        this.myPort    = port;
        this.myPeerId  = myPeerId;
        this.messaging = messaging;
        this.dispatcher = defaultDispatcher();
        this.requestPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true); // pool threads không giữ JVM sống khi GUI đóng
            return t;
        });
    }

    public void setMessageListener(IncomingMessageListener listener) {
        this.messageListener = listener;
    }

    private static PeerMessageDispatcher defaultDispatcher() {
        List<PeerInboundHandler> handlers = Arrays.asList(
                new ChatInboundHandler(),
                new PeerChangedInboundHandler(),
                new RelayRegisterInboundHandler(),
                new RelayInboundHandler(),
                new FileInboundHandler()
        );
        return new PeerMessageDispatcher(handlers);
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            System.out.println("[Hệ thống] Đang lắng nghe tin nhắn tại Port: " + myPort);
            while (true) {
                Socket incomingSocket = serverSocket.accept();
                requestPool.submit(() -> handleIncomingMessage(incomingSocket));
            }
        } catch (IOException e) {
            System.out.println("Lỗi Peer Server: " + e.getMessage());
        } finally {
            requestPool.shutdown();
        }
    }

    private void handleIncomingMessage(Socket socket) {
        try {
            socket.setSoTimeout(READ_TIMEOUT_MS);
        } catch (IOException ignored) {}
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String jsonInput = in.readLine();
            if (jsonInput == null || jsonInput.length() > MAX_MSG_BYTES) return;
            Message msg = gson.fromJson(jsonInput, Message.class);
            PeerHandleContext ctx = new PeerHandleContext(myPeerId, myPort, gson, out, messaging, deduper, messageListener);
            dispatcher.dispatch(msg, ctx);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
