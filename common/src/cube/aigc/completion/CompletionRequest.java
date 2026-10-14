/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.completion;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * Canonical structured messages, tools and generation options.
 */
public final class CompletionRequest {
    private CompletionRequest() {
    }

    public static void validate(JSONObject request) {
        text(request, "model");
        bool(request, "stream");
        JSONArray messages = request.optJSONArray("messages");
        if (messages == null || messages.length() == 0) {
            throw CompletionError.invalid("messages must be a non-empty array", "messages");
        }
        Set<String> pending = new HashSet<>(), seen = new HashSet<>();
        for (int i = 0; i < messages.length(); ++i) {
            JSONObject message = messages.optJSONObject(i);
            if (message == null) throw CompletionError.invalid("Messages must be objects", "messages");
            only(message, "messages", "role", "content", "name", "tool_calls", "tool_call_id", "content_order");
            String role = text(message, "role");
            if (!role.equals("system") && !role.equals("developer") && !role.equals("user")
                    && !role.equals("assistant") && !role.equals("tool")) {
                throw CompletionError.invalid("Unsupported message role", "messages");
            }
            if (!pending.isEmpty() && !role.equals("tool")) {
                throw CompletionError.invalid("Tool results must follow assistant tool calls", "messages");
            }
            JSONArray calls = message.optJSONArray("tool_calls");
            if (message.has("tool_calls") && (calls == null || calls.length() == 0 || !role.equals("assistant"))) {
                throw CompletionError.invalid("Invalid assistant tool calls", "messages");
            }
            Object content = message.opt("content");
            if (content == null || content == JSONObject.NULL) {
                if (calls == null) throw CompletionError.invalid("Message content is required", "messages");
            } else if (content instanceof JSONArray) {
                blocks((JSONArray) content, role.equals("user") || role.equals("tool"));
            } else if (!(content instanceof String)) {
                throw CompletionError.invalid("Invalid message content", "messages");
            }
            if (role.equals("tool") && !pending.remove(text(message, "tool_call_id"))) {
                throw CompletionError.invalid("Tool result has no matching call", "messages");
            }
            if (calls != null) {
                for (int j = 0; j < calls.length(); ++j) {
                    JSONObject call = calls.optJSONObject(j);
                    if (call == null || !"function".equals(call.optString("type"))) {
                        throw CompletionError.invalid("Only function tool calls are supported", "messages");
                    }
                    only(call, "messages", "id", "type", "function");
                    String id = text(call, "id");
                    if (!seen.add(id)) throw CompletionError.invalid("Duplicate tool call ID", "messages");
                    pending.add(id);
                    JSONObject function = call.optJSONObject("function");
                    if (function == null) throw CompletionError.invalid("Missing function", "messages");
                    only(function, "messages", "name", "arguments");
                    text(function, "name");
                    String arguments = text(function, "arguments");
                    try {
                        new JSONObject(arguments);
                    } catch (RuntimeException e) {
                        throw CompletionError.invalid("Tool arguments must be a JSON object", "messages");
                    }
                }
            }
        }
        if (!pending.isEmpty()) throw CompletionError.invalid("All tool calls require results", "messages");
        JSONArray tools = request.optJSONArray("tools");
        Set<String> names = new HashSet<>();
        if (request.has("tools") && tools == null) throw CompletionError.invalid("tools must be an array", "tools");
        if (tools != null) {
            for (int i = 0; i < tools.length(); ++i) {
                JSONObject tool = tools.optJSONObject(i);
                if (tool == null || !"function".equals(tool.optString("type"))) {
                    throw CompletionError.invalid("Only client function tools are supported", "tools");
                }
                JSONObject function = tool.optJSONObject("function");
                only(tool, "tools", "type", "function");
                if (function == null || !names.add(text(function, "name"))) {
                    throw CompletionError.invalid("Invalid or duplicate tool name", "tools");
                }
                only(function, "tools", "name", "description", "parameters", "strict");
                if (function.has("parameters") && function.optJSONObject("parameters") == null) {
                    throw CompletionError.invalid("Tool parameters must be a JSON schema object", "tools");
                }
                bool(function, "strict");
                if (function.optBoolean("strict", false)) {
                    throw CompletionError.invalid("Strict tool schemas are not supported", "tools");
                }
            }
        }
        JSONObject options = request.optJSONObject("options");
        if (options == null) throw CompletionError.invalid("options must be an object", "options");
        for (String key : options.keySet()) {
            Object value = options.get(key);
            if (value == JSONObject.NULL) continue;
            if (key.equals("temperature") || key.equals("top_p") || key.endsWith("penalty")) {
                double min = key.endsWith("penalty") ? -2 : 0;
                double max = key.equals("top_p") ? 1 : 2;
                if (!(value instanceof Number) || !Double.isFinite(((Number) value).doubleValue())
                        || ((Number) value).doubleValue() < min || ((Number) value).doubleValue() > max) {
                    throw CompletionError.invalid("Invalid " + key, key);
                }
            }
            if (key.equals("max_tokens") || key.equals("max_completion_tokens") || key.equals("top_k") || key.equals("seed")) {
                if (!(value instanceof Number) || value instanceof Double || value instanceof Float
                        || (!key.equals("seed") && (key.equals("top_k") ? ((Number) value).longValue() < 0 : ((Number) value).longValue() <= 0))) {
                    throw CompletionError.invalid("Invalid integer " + key, key);
                }
            }
            if (key.equals("parallel_tool_calls")) bool(options, key);
            if (key.equals("stop")) {
                JSONArray stops = value instanceof String ? new JSONArray().put(value) : options.optJSONArray(key);
                if (stops == null || stops.length() < 1 || stops.length() > 4) {
                    throw CompletionError.invalid("stop requires one to four strings", key);
                }
                for (int i = 0; i < stops.length(); ++i) {
                    if (!(stops.get(i) instanceof String) || stops.getString(i).isEmpty()) {
                        throw CompletionError.invalid("stop strings must not be empty", key);
                    }
                }
            }
            if (key.equals("tool_choice")) {
                if (value instanceof String) {
                    if (!value.equals("none") && !value.equals("auto") && !value.equals("required")) {
                        throw CompletionError.invalid("Invalid tool_choice", key);
                    }
                    if (!value.equals("none") && names.isEmpty())
                        throw CompletionError.invalid("tool_choice requires tools", key);
                } else if (value instanceof JSONObject) {
                    JSONObject choice = (JSONObject) value, function = choice.optJSONObject("function");
                    if (!"function".equals(choice.optString("type")) || function == null || !names.contains(function.optString("name"))) {
                        throw CompletionError.invalid("tool_choice must select a declared function", key);
                    }
                } else throw CompletionError.invalid("Invalid tool_choice", key);
            }
        }
        if (options.has("max_tokens") && options.has("max_completion_tokens")
                && options.getLong("max_tokens") != options.getLong("max_completion_tokens")) {
            throw CompletionError.invalid("Conflicting token limits", "max_completion_tokens");
        }
    }

    public static void checkCapabilities(JSONObject request, JSONObject capability) {
        if (!capability.optBoolean("text", false))
            throw CompletionError.invalid("Model does not support structured chat", "model");
        if (request.optBoolean("stream", false) && !capability.optBoolean("stream", false)) {
            throw CompletionError.invalid("Model does not support streaming", "stream");
        }
        if (!capability.optBoolean("images", false)) {
            JSONArray messages = request.getJSONArray("messages");
            for (int i = 0; i < messages.length(); ++i) {
                JSONArray content = messages.getJSONObject(i).optJSONArray("content");
                if (content != null) for (int j = 0; j < content.length(); ++j) {
                    if ("image_url".equals(content.getJSONObject(j).optString("type"))) {
                        throw CompletionError.invalid("Model does not support images", "messages");
                    }
                }
            }
        }
        if (!capability.optBoolean("tools", false)) {
            if (request.optJSONArray("tools") != null && request.getJSONArray("tools").length() > 0) {
                throw CompletionError.invalid("Model does not support tools", "tools");
            }
            JSONArray messages = request.getJSONArray("messages");
            for (int i = 0; i < messages.length(); ++i) {
                JSONObject message = messages.getJSONObject(i);
                if (message.has("tool_calls") || "tool".equals(message.getString("role"))) {
                    throw CompletionError.invalid("Model does not support tools", "messages");
                }
            }
        }
        JSONArray supported = capability.optJSONArray("parameters");
        Set<String> keys = new HashSet<>();
        if (supported != null) for (int i = 0; i < supported.length(); ++i) keys.add(supported.getString(i));
        for (String key : request.getJSONObject("options").keySet()) {
            if (!request.getJSONObject("options").isNull(key) && !keys.contains(key)
                    && !defaultOption(key, request.getJSONObject("options").get(key), request)) {
                throw CompletionError.invalid("Model does not support " + key, key);
            }
        }
    }

    private static boolean defaultOption(String key, Object value, JSONObject request) {
        if ((key.equals("presence_penalty") || key.equals("frequency_penalty")) && value instanceof Number)
            return ((Number) value).doubleValue() == 0;
        boolean noTools = request.optJSONArray("tools") == null || request.getJSONArray("tools").length() == 0;
        return noTools && ((key.equals("tool_choice") && value.equals("none"))
                || (key.equals("parallel_tool_calls") && Boolean.TRUE.equals(value)));
    }

    private static void blocks(JSONArray parts, boolean images) {
        for (int i = 0; i < parts.length(); ++i) {
            JSONObject part = parts.optJSONObject(i);
            if (part == null) throw CompletionError.invalid("Invalid content block", "messages");
            if ("text".equals(part.optString("type"))) {
                only(part, "messages", "type", "text");
                if (!(part.opt("text") instanceof String)) throw CompletionError.invalid("Missing text", "messages");
            } else if (images && "image_url".equals(part.optString("type"))) {
                JSONObject image = part.optJSONObject("image_url");
                only(part, "messages", "type", "image_url");
                if (image == null) throw CompletionError.invalid("Missing image_url", "messages");
                only(image, "messages", "url", "detail");
                String url = text(image, "url");
                try {
                    URI uri = URI.create(url);
                    if (url.startsWith("data:image/")) {
                        int comma = url.indexOf(',');
                        if (comma < 0 || !url.substring(0, comma).matches("data:image/(png|jpeg|gif|webp);base64"))
                            throw new IllegalArgumentException();
                        if (java.util.Base64.getDecoder().decode(url.substring(comma + 1)).length == 0)
                            throw new IllegalArgumentException();
                    }
                    if (!url.startsWith("data:image/") &&
                            !(("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null)) {
                        throw new IllegalArgumentException();
                    }
                } catch (IllegalArgumentException e) {
                    throw CompletionError.invalid("Invalid image URL", "messages");
                }
                String detail = image.optString("detail", "auto");
                if (!detail.equals("auto") && !detail.equals("low") && !detail.equals("high")) {
                    throw CompletionError.invalid("Invalid image detail", "messages");
                }
            } else throw CompletionError.invalid("Unsupported content block", "messages");
        }
    }

    public static String text(JSONObject json, String key) {
        Object value = json.opt(key);
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw CompletionError.invalid(key + " must be a non-empty string", key);
        }
        return (String) value;
    }

    public static void only(JSONObject json, String param, String... keys) {
        Set<String> allowed = new HashSet<>(java.util.Arrays.asList(keys));
        for (String key : json.keySet())
            if (!allowed.contains(key) && !json.isNull(key)) {
                throw CompletionError.invalid("Unsupported parameter: " + key, param);
            }
    }

    public static void bool(JSONObject json, String key) {
        if (json.has(key) && !json.isNull(key) && !(json.get(key) instanceof Boolean)) {
            throw CompletionError.invalid(key + " must be a boolean", key);
        }
    }
}
