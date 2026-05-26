package org.khoicg.chat.tracker;

import com.google.gson.Gson;
import org.khoicg.chat.model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

final class TrackerClientConnection implements Runnable {

    private static final int MAX_MSG_BYTES   = 1_048_576; // 1 MB
    private static final int READ_TIMEOUT_MS = 10_000;    // 10 s

    private final Socket socket;
    private final TrackerState state;
    private final TrackerMessageDispatcher dispatcher;
    private final Gson gson;
    private final TrackerPushBroadcaster broadcaster;

    TrackerClientConnection(Socket socket, TrackerState state,
                            TrackerMessageDispatcher dispatcher, Gson gson,
                            TrackerPushBroadcaster broadcaster) {
        this.socket      = socket;
        this.state       = state;
        this.dispatcher  = dispatcher;
        this.gson        = gson;
        this.broadcaster = broadcaster;
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(READ_TIMEOUT_MS);
        } catch (IOException ignored) {}
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out   = new PrintWriter(socket.getOutputStream(), true)) {
            String jsonInput = in.readLine();
            if (jsonInput == null || jsonInput.length() > MAX_MSG_BYTES) return;
            Message msg = gson.fromJson(jsonInput, Message.class);
            if (msg != null && msg.getSenderId() != null) {
                state.touch(msg.getSenderId());
            }
            TrackerHandleContext ctx = new TrackerHandleContext(state, gson, out, broadcaster);
            dispatcher.dispatch(msg, ctx);
        } catch (IOException ignored) {
            // connection drop hoặc read timeout
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
