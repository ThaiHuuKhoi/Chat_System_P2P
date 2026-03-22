package org.khoicg.chat.peer;

import com.google.gson.Gson;
import org.khoicg.chat.model.Message;
import org.khoicg.chat.net.MessagingClient;
import org.khoicg.chat.peer.handler.ChatInboundHandler;
import org.khoicg.chat.peer.handler.FileInboundHandler;
import org.khoicg.chat.peer.handler.RelayInboundHandler;
import org.khoicg.chat.peer.handler.RelayRegisterInboundHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class PeerServer extends Thread {

    private final int myPort;
    private final String myPeerId;
    private final Gson gson = new Gson();
    private final MessagingClient messaging;
    private final DeliveryIdempotencyTracker deduper = new DeliveryIdempotencyTracker();
    private final PeerMessageDispatcher dispatcher;

    public PeerServer(int port, String myPeerId, MessagingClient messaging) {
        this.myPort = port;
        this.myPeerId = myPeerId;
        this.messaging = messaging;
        this.dispatcher = defaultDispatcher();
    }

    private static PeerMessageDispatcher defaultDispatcher() {
        List<PeerInboundHandler> handlers = Arrays.asList(
                new ChatInboundHandler(),
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
                new Thread(() -> handleIncomingMessage(incomingSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Lỗi Peer Server: " + e.getMessage());
        }
    }

    private void handleIncomingMessage(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String jsonInput = in.readLine();
            if (jsonInput != null) {
                Message msg = gson.fromJson(jsonInput, Message.class);
                PeerHandleContext ctx = new PeerHandleContext(myPeerId, myPort, gson, out, messaging, deduper);
                dispatcher.dispatch(msg, ctx);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
