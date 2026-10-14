/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.dispatcher.aigc.completion;

import cube.aigc.completion.CompletionError;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * No request state is stored on this shared handler.
 */
public final class CompletionHandler extends ContextHandler {
    /** 当前唯一受支持的 Anthropic API 版本。 */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final CompletionBackend backend;
    private final CompletionConfig config;
    private ScheduledThreadPoolExecutor timer;
    private final Set<Session> sessions = Collections.newSetFromMap(new ConcurrentHashMap<Session, Boolean>());

    public CompletionHandler(CompletionBackend backend, CompletionConfig config) {
        super("/v1");
        this.backend = backend;
        this.config = config;
        setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request base, HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
                base.setHandled(true);
                handleRequest(request, response);
            }
        });
    }

    @Override
    protected void doStart() throws Exception {
        timer = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "completion-http-timeout");
            thread.setDaemon(true);
            return thread;
        });
        timer.setRemoveOnCancelPolicy(true);
        timer.scheduleWithFixedDelay(() -> {
            for (Session session : sessions) session.heartbeat();
        }, 1000, 1000, TimeUnit.MILLISECONDS);
        try {
            super.doStart();
        } catch (Exception | Error error) {
            timer.shutdownNow();
            throw error;
        }
    }

    @Override
    protected void doStop() throws Exception {
        for (Session session : sessions) {
            session.fail(new CompletionError(503, "service_unavailable", "HTTP server stopped", null));
            session.close();
        }
        if (timer != null) timer.shutdownNow();
        super.doStop();
    }

    private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        // /messages 只能是 Claude 格式；/models 仅在版本头受支持时才按 Claude 解析，
        // 否则降级为 OpenAI 格式，避免网关透传的未知版本直接 400。
        boolean claude = path.equals("/messages") || (path.startsWith("/models") && claudeVersion(request));
        try {
            if (!config.enabled) throw new CompletionError(404, "not_found", "Compatibility routes are disabled", null);
            if (claude && path.equals("/messages") && !ANTHROPIC_VERSION.equals(request.getHeader("anthropic-version"))) {
                throw CompletionError.invalid("anthropic-version must be " + ANTHROPIC_VERSION, "anthropic-version");
            }
            String token = credential(request);
            if (token == null || token.trim().isEmpty()) throw new CompletionError(401,
                    "authentication_error", "A Cube token is required", null);
            response.setHeader("request-id", UUID.randomUUID().toString());
            if (path.equals("/models") || path.startsWith("/models/")) {
                final boolean nativeFormat = claude;
                if (!request.getMethod().equals("GET"))
                    throw new CompletionError(405, "method_not_allowed", "Use GET for models", null);
                final String credential = token;
                AsyncContext async = request.startAsync();
                async.setTimeout(config.timeoutMs + 1000);
                HttpChannel channel = Request.getBaseRequest(request).getHttpChannel();
                long previousIdle = channel.getIdleTimeout();
                channel.setIdleTimeout(Math.max(previousIdle, config.timeoutMs + 2000));
                async.addListener(new AsyncListener() {
                    @Override
                    public void onComplete(AsyncEvent event) {
                        channel.setIdleTimeout(previousIdle);
                    }

                    @Override
                    public void onTimeout(AsyncEvent event) {
                        writeError(response, new CompletionError(504, "request_timeout", "Model lookup timed out", null), nativeFormat);
                        try {
                            async.complete();
                        } catch (IllegalStateException ignored) {
                        }
                    }

                    @Override
                    public void onError(AsyncEvent event) {
                        channel.setIdleTimeout(previousIdle);
                    }

                    @Override
                    public void onStartAsync(AsyncEvent event) {
                    }
                });
                async.start(() -> {
                    try {
                        JSONArray models = backend.models(credential, config.timeoutMs), data = new JSONArray();
                        if (!path.equals("/models")) {
                            String id = path.substring("/models/".length());
                            JSONObject found = null;
                            for (int i = 0; i < models.length(); ++i)
                                if (id.equals(models.getJSONObject(i).getString("id"))) found = models.getJSONObject(i);
                            if (found == null)
                                throw new CompletionError(404, "model_not_found", "Unknown or unavailable model", "model");
                            json(response, 200, CompletionProtocol.model(found, nativeFormat));
                        } else {
                            for (int i = 0; i < models.length(); ++i)
                                data.put(CompletionProtocol.model(models.getJSONObject(i), nativeFormat));
                            JSONObject result = new JSONObject().put("data", data);
                            if (nativeFormat) result.put("has_more", false)
                                    .put("first_id", data.length() == 0 ? JSONObject.NULL : data.getJSONObject(0).getString("id"))
                                    .put("last_id", data.length() == 0 ? JSONObject.NULL : data.getJSONObject(data.length() - 1).getString("id"));
                            else result.put("object", "list");
                            json(response, 200, result);
                        }
                    } catch (CompletionError e) {
                        writeError(response, e, nativeFormat);
                    } catch (Exception e) {
                        writeError(response, new CompletionError(503, "service_unavailable", "Model service unavailable", null), nativeFormat);
                    } finally {
                        try {
                            async.complete();
                        } catch (IllegalStateException ignored) {
                        }
                    }
                });
                return;
            }
            if (!path.equals("/chat/completions") && !path.equals("/messages"))
                throw new CompletionError(404, "not_found", "Unknown API endpoint", null);
            claude = path.equals("/messages");
            if (!request.getMethod().equals("POST"))
                throw new CompletionError(405, "method_not_allowed", "Use POST for generation", null);
            String type = request.getContentType();
            if (type == null || !type.split(";", 2)[0].trim().equalsIgnoreCase("application/json"))
                throw new CompletionError(415, "invalid_request_error", "Content-Type must be application/json", null);
            JSONObject normalized;
            try {
                normalized = CompletionProtocol.normalize(readBody(request), claude);
            } catch (CompletionError e) {
                throw e;
            } catch (RuntimeException e) {
                throw CompletionError.invalid("Invalid JSON request shape", null);
            }
            String id = (claude ? "msg_" : "chatcmpl-") + UUID.randomUUID().toString().replace("-", "");
            AsyncContext async = request.startAsync();
            // Our timer writes the protocol error before Jetty's final timeout fallback.
            async.setTimeout(config.timeoutMs + 1000);
            Session session = new Session(async, response, normalized, claude, id, Request.getBaseRequest(request).getHttpChannel());
            async.addListener(session);
            sessions.add(session);
            session.timeoutTask = timer.schedule(() -> session.fail(new CompletionError(504,
                    "request_timeout", "Generation timed out", null)), config.timeoutMs, TimeUnit.MILLISECONDS);
            try {
                CompletionBackend.Subscription subscription = backend.generate(token, id, normalized, config.timeoutMs, session);
                session.attach(subscription);
            } catch (CompletionError e) {
                session.fail(e);
            } catch (RuntimeException e) {
                session.fail(new CompletionError(503, "service_unavailable", "Generation service unavailable", null));
            }
        } catch (CompletionError e) {
            json(response, e.status, CompletionProtocol.error(e, claude));
        }
    }

    private static String bearer(String value) {
        return value != null && value.regionMatches(true, 0, "Bearer ", 0, 7) ? value.substring(7).trim() : null;
    }

    /** 版本头存在且等于受支持版本时，才按 Claude 原生格式处理。 */
    private static boolean claudeVersion(HttpServletRequest request) {
        return ANTHROPIC_VERSION.equals(request.getHeader("anthropic-version"));
    }

    /**
     * 提取访问令牌。Claude 走 {@code x-api-key}，OpenAI 走 {@code Authorization: Bearer}；
     * 两者互为回退，客户端只带其中任一种均可通过。
     */
    private static String credential(HttpServletRequest request) {
        String key = request.getHeader("x-api-key"), authorization = bearer(request.getHeader("Authorization"));
        if (key != null && !key.trim().isEmpty()) return key.trim();
        if (authorization != null && !authorization.trim().isEmpty()) return authorization.trim();
        return null;
    }

    private JSONObject readBody(HttpServletRequest request) throws IOException {
        if (request.getContentLengthLong() > config.bodyBytes)
            throw new CompletionError(413, "request_too_large", "Request body exceeds the size limit", null);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        InputStream input = request.getInputStream();
        while ((count = input.read(buffer)) != -1) {
            if ((long) bytes.size() + count > config.bodyBytes)
                throw new CompletionError(413, "request_too_large", "Request body exceeds the size limit", null);
            bytes.write(buffer, 0, count);
        }
        try {
            String body = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
            JSONTokener parser = new JSONTokener(body);
            JSONObject result = new JSONObject(parser);
            if (parser.nextClean() != 0) throw CompletionError.invalid("Trailing content after JSON body", null);
            return result;
        } catch (CharacterCodingException | RuntimeException e) {
            throw CompletionError.invalid("Body must be a UTF-8 JSON object", null);
        }
    }

    private static void json(HttpServletResponse response, int status, JSONObject body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeError(HttpServletResponse response, CompletionError error, boolean claude) {
        try {
            if (!response.isCommitted()) json(response, error.status, CompletionProtocol.error(error, claude));
        } catch (IOException | IllegalStateException ignored) {
        }
    }

    private final class Session implements CompletionBackend.Listener, AsyncListener, WriteListener {
        final AsyncContext async;
        final HttpServletResponse response;
        final boolean streaming, claude;
        final String id, model;
        final long created;
        final CompletionProtocol.Stream encoder;
        final HttpChannel channel;
        final long previousIdle;
        final ArrayDeque<byte[]> queue = new ArrayDeque<>();
        ServletOutputStream output;
        CompletionBackend.Subscription subscription;
        ScheduledFuture<?> timeoutTask;
        int buffered;
        boolean terminal, closed, writer, sse, pumpScheduled;

        Session(AsyncContext async, HttpServletResponse response, JSONObject request, boolean claude, String id, HttpChannel channel) {
            this.async = async;
            this.response = response;
            this.streaming = request.optBoolean("stream");
            this.claude = claude;
            this.id = id;
            this.model = request.getString("model");
            this.created = System.currentTimeMillis() / 1000;
            this.encoder = new CompletionProtocol.Stream(claude, request, id, created);
            this.channel = channel;
            this.previousIdle = channel.getIdleTimeout();
            channel.setIdleTimeout(Math.max(previousIdle, config.timeoutMs + 2000));
        }

        void attach(CompletionBackend.Subscription value) {
            boolean cancel;
            synchronized (this) {
                subscription = value;
                cancel = closed || terminal;
            }
            if (cancel) value.cancel();
        }

        @Override
        public void onEvent(JSONObject event) {
            synchronized (this) {
                if (terminal || closed) return;
                try {
                    String kind = event.getString("kind");
                    JSONObject payload = event.getJSONObject("payload");
                    if (kind.equals("error")) {
                        fail(new CompletionError(payload.getJSONObject("error")));
                        return;
                    }
                    String frame = "";
                    if (kind.equals("completed")) {
                        JSONObject result = payload.getJSONObject("result");
                        frame = streaming ? encoder.complete(result) : CompletionProtocol.result(result, claude, id, model, created).toString();
                        terminal = true;
                    } else if (streaming)
                        frame = kind.equals("started") ? encoder.start() : encoder.delta(payload.getJSONObject("delta"));
                    if (!frame.isEmpty()) {
                        if (streaming) sse = true;
                        enqueue(frame);
                    }
                } catch (CompletionError e) {
                    terminal = false;
                    fail(e);
                } catch (RuntimeException e) {
                    terminal = false;
                    fail(new CompletionError(502, "upstream_error", "Invalid generation event", null));
                }
            }
        }

        synchronized void fail(CompletionError error) {
            if (closed || terminal) return;
            terminal = true;
            cancelBackend();
            // If no bytes have reached the client, return a normal HTTP error.
            if (!response.isCommitted()) {
                queue.clear();
                buffered = 0;
                sse = false;
                response.setStatus(error.status);
                enqueueError(CompletionProtocol.error(error, claude).toString());
            } else {
                queue.clear();
                buffered = 0;
                enqueueError(encoder.failure(error));
            }
        }

        private void enqueueError(String frame) {
            if (frame.getBytes(StandardCharsets.UTF_8).length > config.bufferBytes) close();
            else enqueue(frame);
        }

        private void cancelBackend() {
            if (subscription != null) subscription.cancel();
        }

        synchronized void heartbeat() {
            if (sse && !terminal && !closed) enqueue(": ping\n\n");
        }

        private void enqueue(String frame) {
            byte[] bytes = frame.getBytes(StandardCharsets.UTF_8);
            if (queue.size() >= config.bufferEvents || (long) buffered + bytes.length > config.bufferBytes) {
                terminal = false;
                fail(new CompletionError(503, "buffer_overflow", "Client cannot keep up with generation", null));
                return;
            }
            queue.add(bytes);
            buffered += bytes.length;
            if (pumpScheduled) return;
            pumpScheduled = true;
            try {
                async.start(this::pump);
            } catch (IllegalStateException e) {
                close();
            }
        }

        private synchronized void pump() {
            pumpScheduled = false;
            if (closed) return;
            try {
                if (!writer) {
                    writer = true;
                    output = response.getOutputStream();
                    output.setWriteListener(this);
                }
                drain();
            } catch (IOException | RuntimeException e) {
                close();
            }
        }

        private void drain() throws IOException {
            while (!queue.isEmpty() && output.isReady()) {
                if (!response.isCommitted()) {
                    response.setContentType(sse ? "text/event-stream" : "application/json");
                    response.setCharacterEncoding("UTF-8");
                    if (sse) {
                        response.setHeader("Cache-Control", "no-cache");
                        response.setHeader("X-Accel-Buffering", "no");
                    }
                }
                byte[] bytes = queue.remove();
                buffered -= bytes.length;
                output.write(bytes);
                if (sse && output.isReady()) output.flush();
            }
            if (terminal && queue.isEmpty() && output.isReady()) close();
        }

        private synchronized void close() {
            if (closed) return;
            closed = true;
            queue.clear();
            buffered = 0;
            sessions.remove(this);
            if (timeoutTask != null) timeoutTask.cancel(false);
            channel.setIdleTimeout(previousIdle);
            cancelBackend();
            try {
                async.complete();
            } catch (IllegalStateException ignored) {
            }
        }

        @Override
        public synchronized void onWritePossible() throws IOException {
            if (!closed) drain();
        }

        @Override
        public void onError(Throwable error) {
            close();
        }

        @Override
        public void onComplete(AsyncEvent event) {
            close();
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            fail(new CompletionError(504, "request_timeout", "Generation timed out", null));
            pump();
            close();
        }

        @Override
        public void onError(AsyncEvent event) {
            close();
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
        }
    }
}
