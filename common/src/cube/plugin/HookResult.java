/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

    public void put(String key, Object value) {
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
