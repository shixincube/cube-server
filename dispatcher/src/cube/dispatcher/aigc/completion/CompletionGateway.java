/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.dispatcher.aigc.completion;

import cell.core.talk.dialect.ActionDialect;
import cube.aigc.completion.CompletionError;
import cube.aigc.completion.CompletionResult;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.dispatcher.Director;
import cube.dispatcher.Performer;
import cube.dispatcher.aigc.AIGCCellet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Continuous events bypass Manager's single-response future registry.
 */
public final class CompletionGateway implements CompletionBackend {
    private volatile Performer performer;
    private final Map<String, Pending> requests = new ConcurrentHashMap<>();

    public synchronized void start(Performer performer) {
        this.performer = performer;
    }

    public synchronized void stop() {
        this.performer = null;
        for (Pending pending : requests.values())
            pending.failure(new CompletionError(503,
                    "service_unavailable", "Dispatcher stopped", null));
    }

    private Performer active() {
        Performer current = performer;
        if (current == null) throw new CompletionError(503, "service_unavailable", "Dispatcher is not ready", null);
        return current;
    }

    @Override
    public JSONArray models(String token, long timeoutMs) {
        Performer current = active();
        Director director = current.selectDirector(token, AIGCCellet.NAME);
        ActionDialect dialect = new Packet(AIGCAction.ListChatModels.name, new JSONObject()).toDialect();
        dialect.addParam("token", token);
        ActionDialect reply = current.syncTransmit(director, AIGCCellet.NAME, dialect, timeoutMs);
        if (reply == null) throw new CompletionError(503, "service_unavailable", "Model service unavailable", null);
        JSONObject data = Packet.extractDataPayload(new Packet(reply));
        if (data.has("error")) throw new CompletionError(data.getJSONObject("error"));
        return data.getJSONArray("models");
    }

    @Override
    public synchronized Subscription generate(String token, String id, JSONObject request,
                                              long timeoutMs, Listener listener) {
        Performer current = active();
        if (requests.size() >= 128)
            throw new CompletionError(429, "rate_limit_error", "Dispatcher generation queue is full", null);
        Director director = current.selectDirector(token, AIGCCellet.NAME);
        if (director == null) throw new CompletionError(503, "service_unavailable", "No AI service available", null);
        Pending pending = new Pending(current, director, token, id, listener);
        if (requests.putIfAbsent(id, pending) != null) throw new IllegalStateException("Duplicate request ID");
        ActionDialect dialect = new Packet(AIGCAction.ChatCompletion.name, new JSONObject()
                .put("requestId", id).put("request", request).put("timeoutMs", timeoutMs)).toDialect();
        dialect.addParam("token", token);
        if (!current.tryTransmit(director, AIGCCellet.NAME, dialect)) {
            pending.failure(new CompletionError(503, "service_unavailable", "AI service transport unavailable", null));
        }
        return pending;
    }

    public void onEvent(ActionDialect dialect) {
        JSONObject event = new Packet(dialect).data;
        Pending pending = requests.get(event.optString("requestId"));
        if (pending != null) pending.accept(event);
    }

    public int activeRequests() {
        return requests.size();
    }

    private final class Pending implements Subscription {
        final Performer owner;
        final Director director;
        final String token, id;
        final Listener listener;
        final Object delivery = new Object();
        long sequence;
        boolean terminal;

        Pending(Performer owner, Director director, String token, String id, Listener listener) {
            this.owner = owner;
            this.director = director;
            this.token = token;
            this.id = id;
            this.listener = listener;
        }

        void accept(JSONObject event) {
            synchronized (delivery) {
                try {
                    synchronized (this) {
                        if (terminal) return;
                        long next = CompletionResult.tokens(event, "sequence");
                        if (next == 0) throw new IllegalArgumentException("Invalid event sequence");
                        if (next <= sequence) return;
                        if (next != sequence + 1) throw new IllegalArgumentException("Event sequence interrupted");
                        String kind = event.getString("kind");
                        if (!kind.equals("started") && !kind.equals("delta") && !kind.equals("completed") && !kind.equals("error")) {
                            throw new IllegalArgumentException("Unknown event");
                        }
                        event.getJSONObject("payload");
                        sequence = next;
                        terminal = kind.equals("completed") || kind.equals("error");
                        if (terminal) requests.remove(id, this);
                    }
                    listener.onEvent(event);
                } catch (RuntimeException error) {
                    failure(new CompletionError(502, "upstream_error", "Invalid generation event", null));
                }
            }
        }

        void failure(CompletionError error) {
            synchronized (delivery) {
                JSONObject event;
                synchronized (this) {
                    if (terminal) return;
                    cancel();
                    event = new JSONObject().put("requestId", id).put("sequence", ++sequence)
                            .put("kind", "error").put("payload", new JSONObject().put("error", error.toJSON()));
                }
                listener.onEvent(event);
            }
        }

        @Override
        public synchronized void cancel() {
            if (terminal) return;
            terminal = true;
            requests.remove(id, this);
            ActionDialect dialect = new Packet(AIGCAction.CancelChatCompletion.name,
                    new JSONObject().put("requestId", id)).toDialect();
            dialect.addParam("token", token);
            owner.tryTransmit(director, AIGCCellet.NAME, dialect);
        }
    }
}
