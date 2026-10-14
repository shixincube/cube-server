/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.completion;

import org.json.JSONObject;

/**
 * Transport-independent error; adapters supply each provider's envelope.
 */
public class CompletionError extends RuntimeException {
    public final int status;
    public final String code;
    public final String param;

    public CompletionError(int status, String code, String message, String param) {
        super(message);
        this.status = status;
        this.code = code;
        this.param = param;
    }

    public CompletionError(JSONObject error) {
        this(error.optInt("status", 502), error.optString("code", "upstream_error"),
                error.optString("message", "Generation failed"),
                error.isNull("param") ? null : error.optString("param", null));
    }

    public JSONObject toJSON() {
        return new JSONObject().put("status", status).put("code", code)
                .put("message", getMessage()).put("param", param == null ? JSONObject.NULL : param);
    }

    public static CompletionError invalid(String message, String param) {
        return new CompletionError(400, "invalid_request_error", message, param);
    }
}
