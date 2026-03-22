package org.khoicg.chat.tracker;

import com.google.gson.Gson;
import org.khoicg.chat.model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

final class TrackerClientConnection extends Thread {

    private final Socket socket;
    private final TrackerState state;
    private final TrackerMessageDispatcher dispatcher;
    private final Gson gson;

    TrackerClientConnection(Socket socket, TrackerState state, TrackerMessageDispatcher dispatcher, Gson gson) {
        this.socket = socket;
        this.state = state;
        this.dispatcher = dispatcher;
        this.gson = gson;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String jsonInput = in.readLine();
            if (jsonInput == null) {
                return;
            }
            Message msg = gson.fromJson(jsonInput, Message.class);
            if (msg != null && msg.getSenderId() != null) {
                state.touch(msg.getSenderId());
            }
            TrackerHandleContext ctx = new TrackerHandleContext(state, gson, out);
            dispatcher.dispatch(msg, ctx);
        } catch (IOException ignored) {
            // connection drop
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
