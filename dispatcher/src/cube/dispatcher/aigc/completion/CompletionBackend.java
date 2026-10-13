/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.dispatcher.aigc.completion;

import org.json.JSONArray;
import org.json.JSONObject;

/** Each subscription owns its routing and cancellation state. */
public interface CompletionBackend {

    interface Listener { void onEvent(JSONObject event); }

    interface Subscription { void cancel(); }

    JSONArray models(String token, long timeoutMs);

    Subscription generate(String token, String id, JSONObject request, long timeoutMs, Listener listener);
}
