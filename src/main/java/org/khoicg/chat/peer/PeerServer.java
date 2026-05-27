package org.khoicg.chat.peer;

import com.google.gson.Gson;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.net.MessagingClient;
import org.khoicg.chat.peer.handler.ChatInboundHandler;
import org.khoicg.chat.peer.handler.FileChunkInboundHandler;
import org.khoicg.chat.peer.handler.PeerChangedInboundHandler;
import org.khoicg.chat.peer.handler.RelayInboundHandler;
import org.khoicg.chat.peer.handler.RelayRegisterInboundHandler;
import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.peer.service.FileTransferRegistry;
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


    private final int myPort;
    private final String myPeerId;
    private final Gson gson = new Gson();
    private final MessagingClient messaging;
    private final DeliveryIdempotencyTracker deduper = new DeliveryIdempotencyTracker();
    private final PeerMessageDispatcher dispatcher;
    private final ExecutorService requestPool;
    private volatile IncomingMessageListener messageListener;

    public PeerServer(int port, String myPeerId, MessagingClient messaging,
                      FileTransferRegistry fileRegistry) {
        this.myPort       = port;
        this.myPeerId     = myPeerId;
        this.messaging    = messaging;
        this.dispatcher   = buildDispatcher(fileRegistry);
        this.requestPool  = Executors.newFixedThreadPool(AppConfig.peerServerThreadPoolSize(), r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public void setMessageListener(IncomingMessageListener listener) {
        this.messageListener = listener;
    }

    private static PeerMessageDispatcher buildDispatcher(FileTransferRegistry registry) {
        List<PeerInboundHandler> handlers = Arrays.asList(
                new ChatInboundHandler(),
                new PeerChangedInboundHandler(registry),
                new RelayRegisterInboundHandler(),
                new RelayInboundHandler(),
                new FileChunkInboundHandler(registry)
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
            socket.setSoTimeout(AppConfig.peerServerReadTimeoutMs());
        } catch (IOException ignored) {}
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String jsonInput = in.readLine();
            if (jsonInput == null || jsonInput.length() > AppConfig.peerServerMaxMessageBytes()) return;
            Message msg = gson.fromJson(jsonInput, Message.class);
            PeerHandleContext ctx = new PeerHandleContext(
                    myPeerId, myPort, gson, out, messaging, deduper, messageListener);
            dispatcher.dispatch(msg, ctx);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
