/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.dispatcher.aigc.completion;

import cube.aigc.completion.CompletionError;
import cube.aigc.completion.CompletionRequest;
import cube.aigc.completion.CompletionResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * Provider wire formats are confined to the dispatcher boundary.
 */
public final class CompletionProtocol {
    private static final Set<String> OPTIONS = new HashSet<>(Arrays.asList(
            "max_tokens", "max_completion_tokens", "temperature", "top_p", "top_k", "stop", "seed",
            "presence_penalty", "frequency_penalty", "tool_choice", "parallel_tool_calls"));

    private CompletionProtocol() {
    }

    public static JSONObject normalize(JSONObject body, boolean claude) {
        if (body == null) throw CompletionError.invalid("JSON object body is required", null);
        JSONObject request = new JSONObject().put("model", CompletionRequest.text(body, "model"));
        CompletionRequest.bool(body, "stream");
        request.put("stream", body.optBoolean("stream", false));
        JSONObject options = new JSONObject();
        if (!claude) {
            Set<String> allowed = new HashSet<>(OPTIONS);
            allowed.addAll(Arrays.asList("model", "messages", "stream", "stream_options", "tools", "n",
                    "store", "response_format", "modalities", "user", "logprobs", "top_logprobs"));
            rejectUnknown(body, allowed);
            CompletionRequest.bool(body, "logprobs");
            if (body.optBoolean("logprobs", false))
                throw CompletionError.invalid("Log probabilities are not supported", "logprobs");
            if (body.has("top_logprobs") && !body.isNull("top_logprobs")
                    && (!(body.get("top_logprobs") instanceof Integer) || body.getInt("top_logprobs") != 0)) {
                throw CompletionError.invalid("Log probabilities are not supported", "top_logprobs");
            }
            if (body.has("n") && !body.isNull("n") && (!(body.get("n") instanceof Integer) || body.getInt("n") != 1)) {
                throw CompletionError.invalid("Only n=1 is supported", "n");
            }
            CompletionRequest.bool(body, "store");
            if (body.optBoolean("store", false))
                throw CompletionError.invalid("Stored completions are not supported", "store");
            if (body.has("response_format") && !body.isNull("response_format")) {
                JSONObject format = body.optJSONObject("response_format");
                if (format == null || format.length() != 1 || !"text".equals(format.optString("type"))) {
                    throw CompletionError.invalid("Only text response format is supported", "response_format");
                }
            }
            if (body.has("modalities") && !body.isNull("modalities")) {
                JSONArray modalities = body.optJSONArray("modalities");
                if (modalities == null || modalities.length() != 1 || !"text".equals(modalities.optString(0))) {
                    throw CompletionError.invalid("Only text output is supported", "modalities");
                }
            }
            if (body.has("stream_options") && !body.isNull("stream_options")) {
                JSONObject streamOptions = body.optJSONObject("stream_options");
                if (streamOptions == null || !request.getBoolean("stream"))
                    throw CompletionError.invalid("stream_options requires stream=true", "stream_options");
                rejectUnknown(streamOptions, new HashSet<>(Arrays.asList("include_usage", "include_obfuscation")));
                CompletionRequest.bool(streamOptions, "include_usage");
                CompletionRequest.bool(streamOptions, "include_obfuscation");
                if (streamOptions.optBoolean("include_obfuscation", false))
                    throw CompletionError.invalid("Stream obfuscation is not supported", "stream_options");
                request.put("includeUsage", streamOptions.optBoolean("include_usage", false));
            }
            request.put("messages", body.getJSONArray("messages"));
            JSONArray inputMessages = request.getJSONArray("messages");
            for (int i = 0; i < inputMessages.length(); ++i)
                CompletionRequest.only(inputMessages.getJSONObject(i),
                        "messages", "role", "content", "name", "tool_calls", "tool_call_id");
            if (body.has("tools") && !body.isNull("tools")) request.put("tools", body.getJSONArray("tools"));
            for (String key : OPTIONS) if (body.has(key) && !body.isNull(key)) options.put(key, body.get(key));
        } else {
            rejectUnknown(body, new HashSet<>(Arrays.asList("model", "messages", "system", "max_tokens", "stream",
                    "tools", "tool_choice", "temperature", "top_p", "top_k", "stop_sequences", "thinking")));
            if (!body.has("max_tokens")) throw CompletionError.invalid("max_tokens is required", "max_tokens");
            if (body.has("thinking") && !body.isNull("thinking")) {
                JSONObject thinking = body.optJSONObject("thinking");
                if (thinking == null || thinking.length() != 1 || !"disabled".equals(thinking.optString("type"))) {
                    throw CompletionError.invalid("Extended thinking is not supported", "thinking");
                }
            }
            JSONArray messages = new JSONArray();
            if (body.has("system") && !body.isNull("system")) {
                Object system = body.get("system");
                messages.put(new JSONObject().put("role", "system").put("content", claudeText(system)));
            }
            JSONArray input = body.getJSONArray("messages");
            for (int i = 0; i < input.length(); ++i) claudeMessage(input.getJSONObject(i), messages);
            request.put("messages", messages);
            if (body.has("tools") && !body.isNull("tools")) {
                JSONArray tools = new JSONArray(), inputTools = body.getJSONArray("tools");
                for (int i = 0; i < inputTools.length(); ++i) {
                    JSONObject tool = inputTools.getJSONObject(i);
                    rejectUnknown(tool, new HashSet<>(Arrays.asList("name", "description", "input_schema", "type")));
                    if (tool.has("type") && !"custom".equals(tool.optString("type"))) {
                        throw CompletionError.invalid("Only client function tools are supported", "tools");
                    }
                    JSONObject function = new JSONObject().put("name", CompletionRequest.text(tool, "name"))
                            .put("parameters", tool.getJSONObject("input_schema"));
                    if (tool.has("description")) function.put("description", tool.get("description"));
                    tools.put(new JSONObject().put("type", "function").put("function", function));
                }
                request.put("tools", tools);
            }
            for (String key : Arrays.asList("max_tokens", "temperature", "top_p", "top_k")) {
                if (body.has(key) && !body.isNull(key)) options.put(key, body.get(key));
            }
            if (options.has("temperature") && options.getDouble("temperature") > 1)
                throw CompletionError.invalid("Claude temperature must be between 0 and 1", "temperature");
            if (body.has("stop_sequences") && !body.isNull("stop_sequences")) {
                JSONArray stops = body.getJSONArray("stop_sequences");
                if (stops.length() > 0) options.put("stop", stops);
            }
            if (body.has("tool_choice") && !body.isNull("tool_choice")) {
                JSONObject choice = body.getJSONObject("tool_choice");
                rejectUnknown(choice, new HashSet<>(Arrays.asList("type", "name", "disable_parallel_tool_use")));
                String type = choice.getString("type");
                if (type.equals("tool")) options.put("tool_choice", new JSONObject().put("type", "function")
                        .put("function", new JSONObject().put("name", CompletionRequest.text(choice, "name"))));
                else if (type.equals("any")) options.put("tool_choice", "required");
                else if (type.equals("auto") || type.equals("none")) options.put("tool_choice", type);
                else throw CompletionError.invalid("Unsupported tool_choice", "tool_choice");
                CompletionRequest.bool(choice, "disable_parallel_tool_use");
                if (choice.has("disable_parallel_tool_use"))
                    options.put("parallel_tool_calls", !choice.getBoolean("disable_parallel_tool_use"));
            }
        }
        request.put("options", options);
        CompletionRequest.validate(request);
        // Copy so caller-owned JSON can never be mutated by asynchronous work.
        return new JSONObject(request.toString());
    }

    private static void claudeMessage(JSONObject input, JSONArray output) {
        rejectUnknown(input, new HashSet<>(Arrays.asList("role", "content")));
        String role = input.getString("role");
        if (!role.equals("user") && !role.equals("assistant"))
            throw CompletionError.invalid("Claude messages require user or assistant roles", "messages");
        Object content = input.get("content");
        if (content instanceof String) {
            output.put(new JSONObject().put("role", role).put("content", content));
            return;
        }
        if (!(content instanceof JSONArray)) throw CompletionError.invalid("Invalid content blocks", "messages");
        JSONArray parts = new JSONArray(), calls = new JSONArray(), order = new JSONArray();
        JSONArray blocks = (JSONArray) content;
        for (int i = 0; i < blocks.length(); ++i) {
            JSONObject block = blocks.getJSONObject(i);
            String type = block.getString("type");
            if (type.equals("text")) {
                rejectUnknown(block, new HashSet<>(Arrays.asList("type", "text")));
                parts.put(new JSONObject().put("type", "text").put("text", block.getString("text")));
                order.put(new JSONObject().put("type", "content").put("index", parts.length() - 1));
            } else if (type.equals("image") && role.equals("user")) {
                rejectUnknown(block, new HashSet<>(Arrays.asList("type", "source")));
                JSONObject source = block.getJSONObject("source");
                String url;
                if ("url".equals(source.optString("type"))) url = source.getString("url");
                else if ("base64".equals(source.optString("type"))) {
                    String mime = source.getString("media_type");
                    if (!Arrays.asList("image/png", "image/jpeg", "image/gif", "image/webp").contains(mime)) {
                        throw CompletionError.invalid("Unsupported image media type", "messages");
                    }
                    String data = source.getString("data");
                    try {
                        Base64.getDecoder().decode(data);
                    } catch (IllegalArgumentException e) {
                        throw CompletionError.invalid("Invalid image base64", "messages");
                    }
                    url = "data:" + mime + ";base64," + data;
                } else throw CompletionError.invalid("Unsupported image source", "messages");
                parts.put(new JSONObject().put("type", "image_url").put("image_url", new JSONObject().put("url", url)));
                order.put(new JSONObject().put("type", "content").put("index", parts.length() - 1));
            } else if (type.equals("tool_use") && role.equals("assistant")) {
                rejectUnknown(block, new HashSet<>(Arrays.asList("type", "id", "name", "input")));
                calls.put(new JSONObject().put("type", "function").put("id", CompletionRequest.text(block, "id"))
                        .put("function", new JSONObject().put("name", CompletionRequest.text(block, "name"))
                                .put("arguments", block.getJSONObject("input").toString())));
                order.put(new JSONObject().put("type", "tool_call").put("index", calls.length() - 1));
            } else if (type.equals("tool_result") && role.equals("user")) {
                rejectUnknown(block, new HashSet<>(Arrays.asList("type", "tool_use_id", "content", "is_error")));
                if (parts.length() > 0) {
                    output.put(new JSONObject().put("role", role).put("content", parts));
                    parts = new JSONArray();
                }
                CompletionRequest.bool(block, "is_error");
                Object resultContent = block.opt("content") == null ? "" : block.get("content");
                if (resultContent instanceof JSONArray) {
                    JSONArray converted = new JSONArray();
                    claudeMessage(new JSONObject().put("role", "user").put("content", resultContent), converted);
                    if (converted.length() != 1 || converted.getJSONObject(0).has("tool_call_id"))
                        throw CompletionError.invalid("Invalid tool result content", "messages");
                    resultContent = converted.getJSONObject(0).get("content");
                } else if (!(resultContent instanceof String))
                    throw CompletionError.invalid("Invalid tool result content", "messages");
                if (block.optBoolean("is_error", false)) {
                    if (resultContent instanceof String)
                        resultContent = new JSONObject().put("is_error", true).put("content", resultContent).toString();
                    else
                        ((JSONArray) resultContent).put(new JSONObject().put("type", "text").put("text", "{\"is_error\":true}"));
                }
                output.put(new JSONObject().put("role", "tool").put("tool_call_id", CompletionRequest.text(block, "tool_use_id")).put("content", resultContent));
            } else throw CompletionError.invalid("Unsupported Claude content block", "messages");
        }
        if (parts.length() > 0 || calls.length() > 0 || blocks.length() == 0) {
            JSONObject message = new JSONObject().put("role", role).put("content", parts);
            if (calls.length() > 0) message.put("tool_calls", calls);
            if (calls.length() > 0) message.put("content_order", order);
            output.put(message);
        }
    }

    private static String claudeText(Object content) {
        if (content instanceof String) return (String) content;
        if (!(content instanceof JSONArray)) throw CompletionError.invalid("Text content is required", "messages");
        StringBuilder text = new StringBuilder();
        JSONArray blocks = (JSONArray) content;
        for (int i = 0; i < blocks.length(); ++i) {
            JSONObject block = blocks.getJSONObject(i);
            rejectUnknown(block, new HashSet<>(Arrays.asList("type", "text")));
            if (!"text".equals(block.optString("type")))
                throw CompletionError.invalid("Only text is supported in system and tool results", "messages");
            text.append(block.getString("text"));
        }
        return text.toString();
    }

    private static void rejectUnknown(JSONObject object, Set<String> supported) {
        for (String key : object.keySet()) {
            if (!supported.contains(key) && !object.isNull(key))
                throw CompletionError.invalid("Unsupported parameter: " + key, key);
        }
    }

    public static JSONObject error(CompletionError error, boolean claude) {
        String type = error.code;
        if (claude) {
            switch (error.status) {
                case 401:
                    type = "authentication_error";
                    break;
                case 403:
                    type = "permission_error";
                    break;
                case 404:
                    type = "not_found_error";
                    break;
                case 413:
                    type = "request_too_large";
                    break;
                case 429:
                    type = "rate_limit_error";
                    break;
                case 503:
                    type = "overloaded_error";
                    break;
                default:
                    type = error.status < 500 ? "invalid_request_error" : "api_error";
            }
            return new JSONObject().put("type", "error").put("error", new JSONObject().put("type", type).put("message", error.getMessage()));
        }
        return new JSONObject().put("error", new JSONObject().put("type", error.status < 500 ? "invalid_request_error" : "server_error")
                .put("code", error.code).put("message", error.getMessage())
                .put("param", error.param == null ? JSONObject.NULL : error.param));
    }

    public static JSONObject model(JSONObject model, boolean claude) {
        if (claude) return new JSONObject().put("type", "model").put("id", model.getString("id"))
                .put("display_name", model.optString("name", model.getString("id")))
                .put("created_at", Instant.ofEpochSecond(model.optLong("created", 0)).toString());
        return new JSONObject().put("object", "model").put("id", model.getString("id"))
                .put("created", model.optLong("created", 0)).put("owned_by", "cube")
                .put("capabilities", model.optJSONObject("capabilities") == null ? new JSONObject() : model.getJSONObject("capabilities"));
    }

    public static JSONObject result(JSONObject result, boolean claude, String id, String model, long created) {
        CompletionResult.validate(result);
        if (!claude) return base(id, model, created, false).put("choices", new JSONArray().put(new JSONObject()
                        .put("index", 0).put("message", result.getJSONObject("message"))
                        .put("finish_reason", result.getString("finish_reason")).put("logprobs", JSONObject.NULL)))
                .put("usage", result.getJSONObject("usage"));
        JSONObject message = result.getJSONObject("message");
        JSONArray content = new JSONArray();
        String text = message.optString("content", "");
        if (!text.isEmpty()) content.put(new JSONObject().put("type", "text").put("text", text));
        JSONArray calls = message.optJSONArray("tool_calls");
        if (calls != null) for (int i = 0; i < calls.length(); ++i) content.put(tool(calls.getJSONObject(i)));
        if (result.optJSONArray("content_blocks") != null) content = result.getJSONArray("content_blocks");
        JSONObject usage = result.getJSONObject("usage");
        return new JSONObject().put("type", "message").put("id", id).put("role", "assistant")
                .put("model", model).put("content", content).put("stop_reason", stopReason(result))
                .put("stop_sequence", result.opt("stop_sequence") == null ? JSONObject.NULL : result.get("stop_sequence"))
                .put("usage", new JSONObject().put("input_tokens", usage.getLong("prompt_tokens"))
                        .put("output_tokens", usage.getLong("completion_tokens")));
    }

    static JSONObject tool(JSONObject call) {
        JSONObject function = call.getJSONObject("function");
        return new JSONObject().put("type", "tool_use").put("id", call.getString("id"))
                .put("name", function.getString("name")).put("input", new JSONObject(function.getString("arguments")));
    }

    static String stopReason(JSONObject result) {
        if (result.has("stop_sequence") && !result.isNull("stop_sequence")) return "stop_sequence";
        switch (result.getString("finish_reason")) {
            case "length":
                return "max_tokens";
            case "tool_calls":
                return "tool_use";
            case "content_filter":
                return "refusal";
            default:
                return "end_turn";
        }
    }

    static JSONObject base(String id, String model, long created, boolean chunk) {
        return new JSONObject().put("id", id).put("object", chunk ? "chat.completion.chunk" : "chat.completion")
                .put("model", model).put("created", created);
    }

    /**
     * Stateful encoder belongs to exactly one HTTP request.
     */
    public static class Stream {
        private final boolean claude, includeUsage;
        private final String id, model;
        private final long created;
        private boolean started;
        private int nextBlock;
        private Integer textBlock;
        private final Map<Integer, ToolState> tools = new TreeMap<>();
        private final List<Integer> blocks = new ArrayList<>();
        private final MessageDigest textDigest;
        private long toolBytes;

        public Stream(boolean claude, JSONObject request, String id, long created) {
            this.claude = claude;
            this.includeUsage = request.optBoolean("includeUsage", false);
            this.id = id;
            this.created = created;
            this.model = request.getString("model");
            try {
                textDigest = MessageDigest.getInstance("SHA-256");
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        public String start() {
            if (started) return "";
            started = true;
            if (!claude)
                return openChunk(new JSONObject().put("role", "assistant").put("content", ""), JSONObject.NULL);
            return event("message_start", new JSONObject().put("message", new JSONObject().put("id", id)
                    .put("type", "message").put("role", "assistant").put("model", model).put("content", new JSONArray())
                    .put("stop_reason", JSONObject.NULL).put("stop_sequence", JSONObject.NULL)
                    .put("usage", new JSONObject().put("input_tokens", 0).put("output_tokens", 0))));
        }

        public String delta(JSONObject delta) {
            StringBuilder frames = new StringBuilder(start());
            if (delta.has("role") && !"assistant".equals(delta.getString("role"))) throw invalidStream();
            if (delta.has("content") && !delta.isNull("content")) {
                if (!(delta.get("content") instanceof String)) throw invalidStream();
                textDigest.update(delta.getString("content").getBytes(StandardCharsets.UTF_8));
            }
            if (delta.has("content") && !delta.isNull("content") && !delta.getString("content").isEmpty()) {
                if (claude && textBlock == null) {
                    textBlock = nextBlock++;
                    blocks.add(textBlock);
                    frames.append(event("content_block_start", new JSONObject().put("index", textBlock)
                            .put("content_block", new JSONObject().put("type", "text").put("text", ""))));
                }
                if (claude) frames.append(event("content_block_delta", new JSONObject().put("index", textBlock)
                        .put("delta", new JSONObject().put("type", "text_delta").put("text", delta.getString("content")))));
            }
            JSONArray calls = delta.optJSONArray("tool_calls");
            if (delta.has("tool_calls") && calls == null) throw invalidStream();
            if (calls != null) for (int i = 0; i < calls.length(); ++i) {
                JSONObject part = calls.getJSONObject(i);
                int index = part.getInt("index");
                if (index < 0 || (!tools.containsKey(index) && index != tools.size())) throw invalidStream();
                if (part.has("type") && !"function".equals(part.getString("type"))) throw invalidStream();
                ToolState tool = tools.computeIfAbsent(index, key -> new ToolState());
                if (part.has("id")) {
                    String id = part.getString("id");
                    if (!tool.id.isEmpty() && !tool.id.equals(id)) throw invalidStream();
                    tool.id = id;
                }
                JSONObject function = part.optJSONObject("function");
                if (part.has("function") && function == null) throw invalidStream();
                if (function != null) {
                    String name = function.has("name") ? function.getString("name") : "";
                    String arguments = function.has("arguments") ? function.getString("arguments") : "";
                    if (tool.block != null && !name.isEmpty())
                        throw new CompletionError(502, "upstream_error", "Tool name changed after arguments started", null);
                    tool.name += name;
                    tool.arguments.append(arguments);
                    toolBytes += name.getBytes(StandardCharsets.UTF_8).length + arguments.getBytes(StandardCharsets.UTF_8).length;
                    if (toolBytes > 32L * 1024 * 1024) throw invalidStream();
                    if (claude) frames.append(toolDelta(tool));
                }
            }
            if (!claude) frames.append(openChunk(delta, JSONObject.NULL));
            return frames.toString();
        }

        private CompletionError invalidStream() {
            return new CompletionError(502, "upstream_error", "Inconsistent generation stream", null);
        }

        private String toolDelta(ToolState tool) {
            if (tool.id.isEmpty() || tool.name.isEmpty()) return "";
            StringBuilder frames = new StringBuilder();
            if (tool.block == null) {
                textBlock = null;
                tool.block = nextBlock++;
                blocks.add(tool.block);
                frames.append(event("content_block_start", new JSONObject().put("index", tool.block)
                        .put("content_block", new JSONObject().put("type", "tool_use").put("id", tool.id)
                                .put("name", tool.name).put("input", new JSONObject()))));
            }
            if (tool.arguments.length() > tool.sent) {
                frames.append(event("content_block_delta", new JSONObject().put("index", tool.block)
                        .put("delta", new JSONObject().put("type", "input_json_delta")
                                .put("partial_json", tool.arguments.substring(tool.sent)))));
                tool.sent = tool.arguments.length();
            }
            return frames.toString();
        }

        public String complete(JSONObject result) {
            CompletionResult.validate(result);
            JSONObject message = result.getJSONObject("message");
            byte[] finalText;
            try {
                finalText = MessageDigest.getInstance("SHA-256").digest((message.isNull("content") ? "" : message.getString("content")).getBytes(StandardCharsets.UTF_8));
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
            if (!MessageDigest.isEqual(textDigest.digest(), finalText)) throw invalidStream();
            JSONArray finalCalls = message.optJSONArray("tool_calls");
            if (tools.size() != (finalCalls == null ? 0 : finalCalls.length())) throw invalidStream();
            if (finalCalls != null) for (int i = 0; i < finalCalls.length(); ++i) {
                JSONObject call = finalCalls.getJSONObject(i), function = call.getJSONObject("function");
                ToolState tool = tools.get(i);
                if (tool == null || !tool.id.equals(call.getString("id")) || !tool.name.equals(function.getString("name"))
                        || !tool.arguments.toString().equals(function.getString("arguments"))) throw invalidStream();
            }
            StringBuilder frames = new StringBuilder(start());
            if (!claude) {
                frames.append(openChunk(new JSONObject(), result.getString("finish_reason")));
                if (includeUsage)
                    frames.append(data(base(id, model, created, true).put("choices", new JSONArray()).put("usage", result.getJSONObject("usage"))));
                return frames.append("data: [DONE]\n\n").toString();
            }
            JSONArray calls = result.getJSONObject("message").optJSONArray("tool_calls");
            if (calls != null) for (int i = 0; i < calls.length(); ++i) {
                JSONObject call = calls.getJSONObject(i), function = call.getJSONObject("function");
                ToolState tool = tools.computeIfAbsent(i, key -> new ToolState());
                if (tool.block == null) {
                    tool.id = call.getString("id");
                    tool.name = function.getString("name");
                    tool.arguments = new StringBuilder(function.getString("arguments"));
                    frames.append(toolDelta(tool));
                }
            }
            for (Integer index : blocks)
                frames.append(event("content_block_stop", new JSONObject().put("index", index)));
            JSONObject usage = result.getJSONObject("usage");
            frames.append(event("message_delta", new JSONObject().put("delta", new JSONObject()
                            .put("stop_reason", stopReason(result)).put("stop_sequence", result.opt("stop_sequence") == null ? JSONObject.NULL : result.get("stop_sequence")))
                    .put("usage", new JSONObject().put("input_tokens", usage.getLong("prompt_tokens"))
                            .put("output_tokens", usage.getLong("completion_tokens")))));
            return frames.append(event("message_stop", new JSONObject())).toString();
        }

        public String failure(CompletionError error) {
            return claude ? event("error", CompletionProtocol.error(error, true)) : data(CompletionProtocol.error(error, false));
        }

        private String openChunk(JSONObject delta, Object finish) {
            JSONObject chunk = base(id, model, created, true).put("choices", new JSONArray().put(new JSONObject()
                    .put("index", 0).put("delta", delta).put("finish_reason", finish).put("logprobs", JSONObject.NULL)));
            if (includeUsage) chunk.put("usage", JSONObject.NULL);
            return data(chunk);
        }

        private static String event(String name, JSONObject value) {
            return "event: " + name + "\n" + data(value.put("type", name));
        }

        private static String data(JSONObject value) {
            return "data: " + value.toString() + "\n\n";
        }

        private static class ToolState {
            String id = "", name = "";
            StringBuilder arguments = new StringBuilder();
            Integer block;
            int sent;
        }
    }
}
