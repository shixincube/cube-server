package cube.aigc.completion;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/** Validate terminal data before accounting or committing it to HTTP. */
public final class CompletionResult {
    private CompletionResult() {}
    public static void validate(JSONObject result) {
        try {
            JSONObject message = result.getJSONObject("message");
            if (!"assistant".equals(message.getString("role"))) throw new IllegalArgumentException();
            if (!message.isNull("content") && !(message.get("content") instanceof String)) throw new IllegalArgumentException();
            String finish = result.getString("finish_reason");
            if (!finish.equals("stop") && !finish.equals("length") && !finish.equals("tool_calls") && !finish.equals("content_filter")) throw new IllegalArgumentException();
            JSONArray calls = message.optJSONArray("tool_calls");
            if (message.has("tool_calls") && calls == null) throw new IllegalArgumentException();
            if (finish.equals("tool_calls") && (calls == null || calls.length() == 0)) throw new IllegalArgumentException();
            Set<String> ids = new HashSet<>();
            if (calls != null) for (int i = 0; i < calls.length(); ++i) {
                JSONObject call = calls.getJSONObject(i), function = call.getJSONObject("function");
                if (!"function".equals(call.getString("type")) || !ids.add(CompletionRequest.text(call, "id"))) throw new IllegalArgumentException();
                CompletionRequest.text(function, "name");
                new JSONObject(function.getString("arguments"));
            }
            if (result.has("content_blocks")) {
                JSONArray blocks = result.getJSONArray("content_blocks");
                StringBuilder text = new StringBuilder();
                Set<String> blockIds = new HashSet<>();
                for (int i = 0; i < blocks.length(); ++i) {
                    JSONObject block = blocks.getJSONObject(i);
                    if ("text".equals(block.getString("type"))) text.append(block.getString("text"));
                    else if ("tool_use".equals(block.getString("type"))) {
                        String id = block.getString("id");
                        if (!ids.contains(id) || !blockIds.add(id)) throw new IllegalArgumentException();
                        boolean found = false;
                        if (calls != null) for (int j = 0; j < calls.length(); ++j) {
                            JSONObject call = calls.getJSONObject(j), function = call.getJSONObject("function");
                            if (id.equals(call.getString("id"))) {
                                found = block.getString("name").equals(function.getString("name"))
                                        && block.getJSONObject("input").similar(new JSONObject(function.getString("arguments")));
                            }
                        }
                        if (!found) throw new IllegalArgumentException();
                    } else throw new IllegalArgumentException();
                }
                if (!blockIds.equals(ids) || !text.toString().equals(message.isNull("content") ? "" : message.getString("content"))) throw new IllegalArgumentException();
            }
            if (result.has("stop_sequence") && !result.isNull("stop_sequence")) CompletionRequest.text(result, "stop_sequence");
            JSONObject usage = result.getJSONObject("usage");
            long input = tokens(usage, "prompt_tokens"), output = tokens(usage, "completion_tokens");
            if (tokens(usage, "total_tokens") != Math.addExact(input, output)) throw new IllegalArgumentException();
        } catch (RuntimeException e) { throw new CompletionError(502, "upstream_error", "Invalid generation result or token usage", null); }
    }
    public static long tokens(JSONObject usage, String name) {
        Object value = usage.get(name);
        if (!(value instanceof Integer) && !(value instanceof Long)) throw new IllegalArgumentException("Invalid token usage");
        long count = ((Number) value).longValue();
        if (count < 0) throw new IllegalArgumentException("Invalid token usage");
        return count;
    }
}
