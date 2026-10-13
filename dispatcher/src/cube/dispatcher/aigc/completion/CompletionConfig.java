/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.dispatcher.aigc.completion;

import java.util.Properties;

public final class CompletionConfig {

    public final boolean enabled;
    public final long timeoutMs;
    public final int bodyBytes, bufferEvents, bufferBytes;

    public CompletionConfig(Properties properties) {
        Properties p = properties == null ? new Properties() : properties;
        enabled = Boolean.parseBoolean(p.getProperty("completion.rest.enabled", "true"));
        timeoutMs = positive(p, "completion.rest.timeout.seconds", 300) * 1000L;
        bodyBytes = positive(p, "completion.rest.body.bytes", 32 * 1024 * 1024);
        bufferEvents = positive(p, "completion.rest.buffer.events", 256);
        bufferBytes = positive(p, "completion.rest.buffer.bytes", 4 * 1024 * 1024);
    }

    private static int positive(Properties p, String key, int fallback) {
        int value = Integer.parseInt(p.getProperty(key, String.valueOf(fallback)));
        if (value <= 0) throw new IllegalArgumentException(key + " must be positive");
        return value;
    }
}
