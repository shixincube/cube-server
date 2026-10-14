package cube.dispatcher.test;

import cube.aigc.completion.CompletionError;
import cube.dispatcher.aigc.completion.CompletionBackend;
import cube.dispatcher.aigc.completion.CompletionConfig;
import cube.dispatcher.aigc.completion.CompletionHandler;
import cube.dispatcher.aigc.completion.CompletionProtocol;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real Jetty HTTP fixture with deterministic events; also used by official SDK tests.
 */
public final class CompletionHttpTest {
    static int assertions;

    static void check(boolean value, String message) {
        ++assertions;
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        FakeBackend backend = new FakeBackend();
        Properties properties = new Properties();
        properties.setProperty("completion.rest.timeout.seconds", args.length > 0 && args[0].equals("serve") ? "10" : "2");
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(args.length > 1 ? Integer.parseInt(args[1]) : 0);
        connector.setIdleTimeout(100); // Requests extend and restore the legacy connector timeout.
        server.addConnector(connector);
        ContextHandler state = new ContextHandler("/test/state");
        state.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request base, HttpServletRequest request, HttpServletResponse response) throws IOException {
                base.setHandled(true);
                response.setContentType("application/json");
                response.getWriter().write(new JSONObject().put("active", backend.active.get())
                        .put("cancelled", backend.cancelled.get()).put("completed", backend.completed.get())
                        .put("requests", new JSONArray(backend.seen)).toString());
            }
        });
        ContextHandlerCollection handlers = new ContextHandlerCollection();
        Properties limitProperties = new Properties();
        limitProperties.putAll(properties);
        limitProperties.setProperty("completion.rest.body.bytes", "512");
        CompletionHandler limits = new CompletionHandler(backend, new CompletionConfig(limitProperties));
        limits.setContextPath("/limits/v1");
        handlers.setHandlers(new Handler[]{new CompletionHandler(backend, new CompletionConfig(properties)), limits, state});
        server.setHandler(handlers);
        server.start();
        String url = "http://127.0.0.1:" + connector.getLocalPort();
        if (args.length > 0 && args[0].equals("serve")) {
            System.out.println("COMPAT_URL=" + url);
            System.out.flush();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> backend.executor.shutdownNow()));
            server.join();
            return;
        }
        try {
            protocolTests();
            httpTests(url, backend);
            HttpURLConnection pending = open(url + "/v1/chat/completions", body("idle-after", true));
            pending.getInputStream().read();
            server.stop();
            check(backend.active.get() == 0, "HTTP server stop cancels open generation");
            pending.disconnect();
            System.out.println("CompletionHttpTest: " + assertions + " assertions passed");
        } finally {
            backend.executor.shutdownNow();
            server.stop();
        }
    }

    static JSONObject body(String text, boolean stream) {
        return new JSONObject().put("model", "Baize2").put("stream", stream).put("messages",
                new JSONArray().put(new JSONObject().put("role", "user").put("content", text)));
    }

    static void protocolTests() {
        JSONObject request = new JSONObject("{\"model\":\"Baize2\",\"max_tokens\":30,\"system\":[{\"type\":\"text\",\"text\":\"sys\"}],\"messages\":[{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"a\"},{\"type\":\"image\",\"source\":{\"type\":\"url\",\"url\":\"https://example.com/1.png\"}}]},{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"before\"},{\"type\":\"tool_use\",\"id\":\"call_a\",\"name\":\"lookup\",\"input\":{\"q\":\"x\"}},{\"type\":\"text\",\"text\":\"after\"}]},{\"role\":\"user\",\"content\":[{\"type\":\"tool_result\",\"tool_use_id\":\"call_a\",\"content\":[{\"type\":\"text\",\"text\":\"result\"},{\"type\":\"image\",\"source\":{\"type\":\"url\",\"url\":\"https://example.com/2.png\"}}]},{\"type\":\"text\",\"text\":\"continue\"}]}]}");
        JSONObject normalized = CompletionProtocol.normalize(request, true);
        JSONArray messages = normalized.getJSONArray("messages");
        check(messages.getJSONObject(0).getString("content").equals("sys"), "system context");
        check(messages.length() == 5, "message order and tool results");
        check(messages.getJSONObject(2).getJSONArray("content_order").getJSONObject(1).getString("type").equals("tool_call"), "interleaved content order");
        check(messages.getJSONObject(3).getJSONArray("content").getJSONObject(1).getJSONObject("image_url").getString("url").endsWith("2.png"), "tool result image");
        for (JSONObject invalid : Arrays.asList(body("x", false).put("n", 2), body("x", false).put("response_format", new JSONObject().put("type", "json_object")))) {
            try {
                CompletionProtocol.normalize(invalid, false);
                throw new AssertionError("Unsupported option accepted");
            } catch (CompletionError error) {
                check(error.status == 400, "parameter status");
            }
        }
        for (String finish : Arrays.asList("length", "content_filter", "stop")) {
            JSONObject terminal = new JSONObject().put("message", new JSONObject().put("role", "assistant").put("content", ""))
                    .put("finish_reason", finish).put("usage", new JSONObject().put("prompt_tokens", 3).put("completion_tokens", 1).put("total_tokens", 4));
            if (finish.equals("stop")) terminal.put("stop_sequence", "END");
            String expected = finish.equals("length") ? "max_tokens" : finish.equals("content_filter") ? "refusal" : "stop_sequence";
            check(CompletionProtocol.result(terminal, true, "msg_test", "Baize2", 1).getString("stop_reason").equals(expected), "finish reason mapping");
        }
        JSONObject interleaved = new JSONObject().put("message", new JSONObject().put("role", "assistant").put("content", "beforeafter")
                        .put("tool_calls", new JSONArray().put(new JSONObject().put("type", "function").put("id", "c1")
                                .put("function", new JSONObject().put("name", "f").put("arguments", "{}")))))
                .put("finish_reason", "tool_calls").put("usage", new JSONObject().put("prompt_tokens", 3).put("completion_tokens", 1).put("total_tokens", 4))
                .put("content_blocks", new JSONArray().put(new JSONObject().put("type", "text").put("text", "before"))
                        .put(new JSONObject().put("type", "tool_use").put("id", "c1").put("name", "f").put("input", new JSONObject()))
                        .put(new JSONObject().put("type", "text").put("text", "after")));
        check(CompletionProtocol.result(interleaved, true, "msg_test", "Baize2", 1).getJSONArray("content").getJSONObject(1).getString("type").equals("tool_use"), "terminal response preserves content order");
    }

    /** 打开一个 Claude 格式请求（x-api-key + anthropic-version）。 */
    static HttpURLConnection claude(String url, JSONObject body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setReadTimeout(5000);
        connection.setRequestProperty("x-api-key", "test-token");
        connection.setRequestProperty("anthropic-version", "2023-06-01");
        if (body != null) {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        return connection;
    }

    /**
     * 校验 Claude SSE 的 content_block 严格嵌套：每个 start 必须先于其 delta，
     * 且下一个 start 之前必须已收到上一个块的 stop。
     */
    static void checkBlockNesting(String stream, String label) {
        int open = -1, index = -1, stops = 0, starts = 0;
        for (String line : stream.split("\n")) {
            if (!line.startsWith("data: ")) continue;
            JSONObject frame = new JSONObject(line.substring(6));
            String type = frame.optString("type");
            if ("content_block_start".equals(type)) {
                check(open < 0, label + ": start must not overlap an open block");
                open = index = frame.getInt("index");
                ++starts;
                check(index == starts - 1, label + ": block index must be sequential");
            } else if ("content_block_delta".equals(type)) {
                check(open >= 0 && frame.getInt("index") == open, label + ": delta must target the open block");
            } else if ("content_block_stop".equals(type)) {
                check(open >= 0 && frame.getInt("index") == open, label + ": stop must close the open block");
                open = -1;
                ++stops;
            }
        }
        check(open < 0, label + ": every started block must be closed");
        check(starts > 0 && starts == stops, label + ": start/stop count must match");
    }

    static HttpURLConnection open(String url, JSONObject body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setReadTimeout(5000);
        connection.setRequestProperty("Authorization", "Bearer test-token");
        if (body != null) {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        return connection;
    }

    static String read(HttpURLConnection connection) throws IOException {
        InputStream input = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line).append('\n');
        } finally {
            connection.disconnect();
        }
        return result.toString();
    }

    static void httpTests(String url, FakeBackend backend) throws Exception {
        HttpURLConnection oversize = open(url + "/limits/v1/chat/completions", body(String.join("", Collections.nCopies(1000, "x")), false));
        check(oversize.getResponseCode() == 413, "fixed-length body limit");
        read(oversize);
        HttpURLConnection chunked = (HttpURLConnection) new URL(url + "/limits/v1/chat/completions").openConnection();
        chunked.setRequestMethod("POST");
        chunked.setDoOutput(true);
        chunked.setChunkedStreamingMode(64);
        chunked.setRequestProperty("Content-Type", "application/json");
        chunked.setRequestProperty("Authorization", "Bearer test-token");
        try (OutputStream output = chunked.getOutputStream()) {
            output.write(body(String.join("", Collections.nCopies(1000, "x")), false).toString().getBytes(StandardCharsets.UTF_8));
        }
        check(chunked.getResponseCode() == 413, "chunked body limit");
        read(chunked);
        check(new JSONObject(read(open(url + "/v1/models", null))).getJSONArray("data").length() == 1, "model list");
        HttpURLConnection invalid = open(url + "/v1/chat/completions", body("x", false).put("model", "missing"));
        check(invalid.getResponseCode() == 404, "unknown model");
        read(invalid);
        JSONObject result = new JSONObject(read(open(url + "/v1/chat/completions", body("ordinary", false))));
        check(result.getJSONObject("usage").getInt("prompt_tokens") == 11, "real upstream usage forwarded");
        HttpURLConnection stream = open(url + "/v1/chat/completions", body("first", true));
        int finishedBefore = backend.completed.get();
        boolean arrived = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("echo:first")) {
                    arrived = true;
                    check(backend.completed.get() == finishedBefore, "content arrives before completion");
                }
            }
        } finally {
            stream.disconnect();
        }
        check(arrived, "content delta was emitted");
        ExecutorService clients = Executors.newFixedThreadPool(2);
        try {
            Future<String> a = clients.submit(() -> read(open(url + "/v1/chat/completions", body("alpha", true))));
            Future<String> b = clients.submit(() -> read(open(url + "/v1/chat/completions", body("beta", true))));
            check(a.get().contains("echo:alpha") && !a.get().contains("echo:beta"), "first stream isolation");
            check(b.get().contains("echo:beta") && !b.get().contains("echo:alpha"), "second stream isolation");
        } finally {
            clients.shutdownNow();
        }
        HttpURLConnection failure = open(url + "/v1/chat/completions", body("fail-before", true));
        check(failure.getResponseCode() == 502, "pre-stream failure HTTP status");
        check(read(failure).contains("upstream_error"), "pre-stream error body");
        check(read(open(url + "/v1/chat/completions", body("fail-after", true))).contains("upstream_error"), "mid-stream error event");
        HttpURLConnection timeout = open(url + "/v1/chat/completions", body("timeout", true));
        check(timeout.getResponseCode() == 504, "timeout JSON status");
        read(timeout);
        check(backend.active.get() == 0, "all completed and cancelled requests released");
        HttpURLConnection overflow = open(url + "/v1/chat/completions", body("overflow", true));
        String overflowResult = read(overflow);
        check(overflowResult.contains("buffer_overflow"), "bounded buffer rejects oversized events");
        check(backend.active.get() == 0, "overflow cancellation");
        claudeTests(url);
    }

    /** Claude 格式端到端：纯文本、文本+工具混合、终态不带 content_blocks。 */
    static void claudeTests(String url) throws Exception {
        // Claude 格式强制要求 max_tokens，与 OpenAI 不同。
        String plain = read(claude(url + "/v1/messages", body("claude-text", true).put("max_tokens", 64)));
        check(plain.contains("event: message_start"), "claude stream opens with message_start");
        check(plain.contains("event: message_stop"), "claude stream ends with message_stop");
        check(plain.contains("echo:claude-text"), "claude text delta forwarded");
        checkBlockNesting(plain, "claude plain text");

        // 终态不带 content_blocks：文本块必须由 message.content 重建并正确收尾。
        String mixed = read(claude(url + "/v1/messages", body("mixed", true).put("max_tokens", 64)));
        checkBlockNesting(mixed, "claude mixed");
        check(mixed.contains("\"text\":\"before\""), "mixed keeps streamed text");
        check(mixed.contains("\"tool_use\""), "mixed emits tool_use block");
        check(mixed.contains("\"stop_reason\":\"tool_use\""), "tool_calls maps to tool_use stop reason");
        check(mixed.indexOf("echo") < 0, "mixed must not echo the prompt");

        // 纯工具调用（content 为 null）不得凭空造出文本块。
        String toolsOnly = read(claude(url + "/v1/messages", body("claude-tools", true).put("max_tokens", 64)));
        checkBlockNesting(toolsOnly, "claude tools only");

        HttpURLConnection claudeModels = (HttpURLConnection) new URL(url + "/v1/models").openConnection();
        claudeModels.setReadTimeout(5000);
        claudeModels.setRequestProperty("x-api-key", "test-token");
        claudeModels.setRequestProperty("anthropic-version", "2023-06-01");
        JSONObject nativeModels = new JSONObject(read(claudeModels));
        check("model".equals(nativeModels.getJSONArray("data").getJSONObject(0).getString("type")),
                "claude model list uses native model type");
        check(nativeModels.has("has_more") && nativeModels.has("first_id"), "claude model list pagination fields");

        // 未知 anthropic-version 必须降级为 OpenAI 格式，而不是 400。
        HttpURLConnection downgraded = (HttpURLConnection) new URL(url + "/v1/models").openConnection();
        downgraded.setReadTimeout(5000);
        downgraded.setRequestProperty("x-api-key", "test-token");
        downgraded.setRequestProperty("anthropic-version", "2023-01-01");
        JSONObject fallback = new JSONObject(read(downgraded));
        check("model".equals(fallback.getJSONArray("data").getJSONObject(0).getString("object")),
                "unknown anthropic-version falls back to the OpenAI shape");
        check(!fallback.has("has_more"), "fallback omits Anthropic pagination fields");

        // /messages 不接受降级：版本头非法仍须 400。
        HttpURLConnection bad = (HttpURLConnection) new URL(url + "/v1/messages").openConnection();
        bad.setReadTimeout(5000);
        bad.setRequestMethod("POST");
        bad.setDoOutput(true);
        bad.setRequestProperty("Content-Type", "application/json");
        bad.setRequestProperty("x-api-key", "test-token");
        bad.setRequestProperty("anthropic-version", "1999-01-01");
        try (OutputStream output = bad.getOutputStream()) {
            output.write(body("x", true).put("max_tokens", 8).toString().getBytes(StandardCharsets.UTF_8));
        }
        check(bad.getResponseCode() == 400, "/messages rejects an unsupported anthropic-version");
        check(read(bad).contains("anthropic-version"), "rejection names the offending header");
    }

    public static final class FakeBackend implements CompletionBackend {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
        final AtomicInteger active = new AtomicInteger(), completed = new AtomicInteger(), cancelled = new AtomicInteger();
        final List<JSONObject> seen = new CopyOnWriteArrayList<>();

        @Override
        public JSONArray models(String token, long timeoutMs) {
            auth(token);
            return new JSONArray().put(new JSONObject().put("id", "Baize2").put("name", "Baize2"));
        }

        static void auth(String token) {
            if (!token.equals("test-token"))
                throw new CompletionError(401, "authentication_error", "Invalid token", null);
        }

        @Override
        public Subscription generate(String token, String id, JSONObject request, long timeoutMs, Listener listener) {
            auth(token);
            if (!request.getString("model").equals("Baize2"))
                throw new CompletionError(404, "model_not_found", "Unknown model", "model");
            seen.add(new JSONObject(request.toString()));
            active.incrementAndGet();
            AtomicBoolean terminal = new AtomicBoolean();
            AtomicLong sequence = new AtomicLong();
            Subscription cancel = () -> {
                if (terminal.compareAndSet(false, true)) {
                    active.decrementAndGet();
                    cancelled.incrementAndGet();
                }
            };
            java.util.function.BiConsumer<String, JSONObject> emit = (kind, payload) -> {
                if (terminal.get()) return;
                if (kind.equals("completed") || kind.equals("error")) {
                    if (!terminal.compareAndSet(false, true)) return;
                    active.decrementAndGet();
                    completed.incrementAndGet();
                }
                listener.onEvent(new JSONObject().put("requestId", id).put("sequence", sequence.incrementAndGet()).put("kind", kind).put("payload", payload));
            };
            JSONArray messages = request.getJSONArray("messages");
            JSONObject last = messages.getJSONObject(messages.length() - 1);
            String text = last.optString("content", "images received");
            if (text.equals("timeout")) return cancel;
            executor.schedule(() -> {
                if (text.equals("fail-before")) {
                    emit.accept("error", new JSONObject().put("error", new CompletionError(502, "upstream_error", "Controlled failure", null).toJSON()));
                    return;
                }
                boolean tools = request.optJSONArray("tools") != null && request.getJSONArray("tools").length() > 0 && !last.getString("role").equals("tool");
                String answer = "echo:" + text;
                boolean mixed = text.equals("mixed");
                JSONObject message = new JSONObject().put("role", "assistant").put("content", (tools || mixed) ? JSONObject.NULL : answer);
                if (mixed) {
                    message.put("content", "before");
                    message.put("tool_calls", new JSONArray().put(new JSONObject().put("id", "call_test").put("type", "function")
                            .put("function", new JSONObject().put("name", "lookup").put("arguments", "{\"city\":\"Beijing\"}"))));
                }
                if (tools) {
                    String name = request.getJSONArray("tools").getJSONObject(0).getJSONObject("function").getString("name");
                    message.put("tool_calls", new JSONArray().put(new JSONObject().put("id", "call_test").put("type", "function")
                            .put("function", new JSONObject().put("name", name).put("arguments", "{\"city\":\"Beijing\"}"))));
                }
                if (request.optBoolean("stream")) {
                    emit.accept("started", new JSONObject());
                    if (text.equals("overflow")) {
                        char[] huge = new char[4 * 1024 * 1024 + 1];
                        Arrays.fill(huge, 'x');
                        emit.accept("delta", new JSONObject().put("delta", new JSONObject().put("content", new String(huge))));
                        return;
                    }
                    if (text.equals("mixed")) {
                        // 先文本、再工具：用于验证 content_block 严格嵌套（stop 必须先于下一个 start）。
                        emit.accept("delta", new JSONObject().put("delta", new JSONObject().put("content", "before")));
                        emit.accept("delta", new JSONObject().put("delta", new JSONObject().put("tool_calls", new JSONArray().put(new JSONObject()
                                .put("index", 0).put("id", "call_test").put("type", "function")
                                .put("function", new JSONObject().put("name", "lookup").put("arguments", "{\"city\":\"Beijing\"}"))))));
                    } else if (tools) {
                        JSONObject call = message.getJSONArray("tool_calls").getJSONObject(0);
                        emit.accept("delta", new JSONObject().put("delta", new JSONObject().put("tool_calls", new JSONArray().put(new JSONObject()
                                .put("index", 0).put("id", "call_test").put("type", "function")
                                .put("function", new JSONObject().put("name", call.getJSONObject("function").getString("name")).put("arguments", "{\"city\":"))))));
                        executor.schedule(() -> emit.accept("delta", new JSONObject().put("delta", new JSONObject().put("tool_calls", new JSONArray()
                                .put(new JSONObject().put("index", 0).put("function", new JSONObject().put("arguments", "\"Beijing\"}")))))), 100, TimeUnit.MILLISECONDS);
                    } else emit.accept("delta", new JSONObject().put("delta", new JSONObject().put("content", answer)));
                }
                if (text.equals("idle-after")) return;
                executor.schedule(() -> {
                    if (text.equals("fail-after"))
                        emit.accept("error", new JSONObject().put("error", new CompletionError(502, "upstream_error", "Controlled mid-stream failure", null).toJSON()));
                    else
                        emit.accept("completed", new JSONObject().put("result", new JSONObject().put("message", message)
                                .put("finish_reason", (tools || mixed) ? "tool_calls" : "stop").put("usage", new JSONObject().put("prompt_tokens", 11).put("completion_tokens", 3).put("total_tokens", 14))));
                }, 350, TimeUnit.MILLISECONDS);
            }, 20, TimeUnit.MILLISECONDS);
            return cancel;
        }
    }
}
