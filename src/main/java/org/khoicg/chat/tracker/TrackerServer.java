package org.khoicg.chat.tracker;

import com.google.gson.Gson;
import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.config.AppConfig;
import org.khoicg.chat.tracker.handler.GetKnownPeersTrackerHandler;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TrackerServer {


    private final int port;
    private final TrackerState state;
    private final TrackerMessageDispatcher dispatcher;
    private final Gson gson = new Gson();
    private final TrackerPushBroadcaster broadcaster;
    private final ExecutorService requestPool = Executors.newFixedThreadPool(AppConfig.trackerServerThreadPoolSize());

    public TrackerServer(int port, TrackerState state, TrackerMessageDispatcher dispatcher,
                         TrackerPushBroadcaster broadcaster) {
        this.port        = port;
        this.state       = state;
        this.dispatcher  = dispatcher;
        this.broadcaster = broadcaster;
    }

    public static void main(String[] args) {
        int port = AppConfig.trackerPort();
        TrackerState state = new TrackerState();
        TrackerPushBroadcaster broadcaster = new TrackerPushBroadcaster();
        TrackerMessageDispatcher dispatcher = defaultDispatcher();
        System.out.println("=== Tracker Server (bootstrap) — port " + port + " ===");
        TrackerStalePeerMonitor monitor = new TrackerStalePeerMonitor(state, broadcaster);
        Thread t = new Thread(monitor);
        t.setDaemon(true);
        t.start();
        new TrackerServer(port, state, dispatcher, broadcaster).runAcceptLoop();
    }

    static TrackerMessageDispatcher defaultDispatcher() {
        List<TrackerMessageHandler> handlers = Arrays.asList(
                new RegisterTrackerHandler(),
                new GetPeersTrackerHandler(),
                new GetKnownPeersTrackerHandler(),
                new HeartbeatTrackerHandler(),
                new QuitTrackerHandler(),
                new StoreOfflineTrackerHandler(),
                new PullOfflineTrackerHandler()
        );
        return new TrackerMessageDispatcher(handlers);
    }

    private void runAcceptLoop() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                requestPool.submit(
                        new TrackerClientConnection(clientSocket, state, dispatcher, gson, broadcaster));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            requestPool.shutdown();
        }
    }
}
