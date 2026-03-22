package org.khoicg.chat.tracker;

import com.google.gson.Gson;
import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.tracker.handler.ChordFingerTrackerHandler;
import org.khoicg.chat.tracker.handler.ChordLookupTrackerHandler;
import org.khoicg.chat.tracker.handler.ChordRingTrackerHandler;
import org.khoicg.chat.tracker.handler.GetPeersTrackerHandler;
import org.khoicg.chat.tracker.handler.HeartbeatTrackerHandler;
import org.khoicg.chat.tracker.handler.PullOfflineTrackerHandler;
import org.khoicg.chat.tracker.handler.QuitTrackerHandler;
import org.khoicg.chat.tracker.handler.RegisterTrackerHandler;
import org.khoicg.chat.tracker.handler.StoreOfflineTrackerHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public final class TrackerServer {

    private final int port;
    private final TrackerState state;
    private final TrackerMessageDispatcher dispatcher;
    private final Gson gson = new Gson();

    public TrackerServer(int port, TrackerState state, TrackerMessageDispatcher dispatcher) {
        this.port = port;
        this.state = state;
        this.dispatcher = dispatcher;
    }

    public static void main(String[] args) {
        int port = AppConfig.trackerPort();
        TrackerState state = new TrackerState(AppConfig.chordIdentifierBits());
        TrackerMessageDispatcher dispatcher = defaultDispatcher();
        System.out.println("=== Tracker Server (bootstrap) — port " + port + " ===");
        TrackerStalePeerMonitor monitor = new TrackerStalePeerMonitor(state);
        Thread t = new Thread(monitor);
        t.setDaemon(true);
        t.start();
        new TrackerServer(port, state, dispatcher).runAcceptLoop();
    }

    static TrackerMessageDispatcher defaultDispatcher() {
        List<TrackerMessageHandler> handlers = Arrays.asList(
                new RegisterTrackerHandler(),
                new GetPeersTrackerHandler(),
                new HeartbeatTrackerHandler(),
                new QuitTrackerHandler(),
                new StoreOfflineTrackerHandler(),
                new PullOfflineTrackerHandler(),
                new ChordRingTrackerHandler(),
                new ChordFingerTrackerHandler(),
                new ChordLookupTrackerHandler()
        );
        return new TrackerMessageDispatcher(handlers);
    }

    private void runAcceptLoop() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new TrackerClientConnection(clientSocket, state, dispatcher, gson).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
