/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hook 处理结果。
 */
public class HookResult {

    private List<HookResult> results;

    private Map<String, Object> data;

    public HookResult() {
        this.results = new ArrayList<>();
    }

    public void add(HookResult result) {
        this.results.add(result);
    }

    public void set(String key, Object value) {
        if (null == this.data) {
            this.data = new HashMap<>();
        }
        this.data.put(key, value);
    }

    public Object get(String key) {
        if (null != this.data) {
            Object value = this.data.get(key);
            if (null != value) {
                return value;
            }
        }

        for (HookResult hr : this.results) {
            Object value = hr.get(key);
            if (null != value) {
                return value;
            }
        }

        return null;
    }

    public boolean getBoolean(String key) {
        Object value = this.get(key);
        if (null == value) {
            return false;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        else {
            return false;
        }
    }
}
