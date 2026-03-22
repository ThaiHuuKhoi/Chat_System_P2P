package org.khoicg.chat.tracker;

import com.google.gson.Gson;
import org.khoicg.chat.model.Message;

import java.io.PrintWriter;

public final class TrackerHandleContext {

    private final TrackerState state;
    private final Gson gson;
    private final PrintWriter out;

    public TrackerHandleContext(TrackerState state, Gson gson, PrintWriter out) {
        this.state = state;
        this.gson = gson;
        this.out = out;
    }

    public TrackerState state() {
        return state;
    }

    public Gson gson() {
        return gson;
    }

    public void reply(Message response) {
        out.println(gson.toJson(response));
    }
}
