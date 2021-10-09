/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

/**
 * 事件钩子。
 */
public class Hook {

    /**
     * 触发钩子的关键字。
     */
    private String key;

    /**
     * 钩子的系统宿主。
     */
    protected PluginSystem system;

    /**
     * 构造函数。
     *
     * @param key
     */
    public Hook(String key) {
        this.key = key;
    }

    /**
     * 获取对应的触发关键字。
     *
     * @return 返回对应的触发关键字。
     */
    public String getKey() {
        return this.key;
    }

    /**
     * 触发插件回调。
     *
     * @param context
     */
    public void apply(PluginContext context) {
        context.setKey(this.key);

        this.system.apply(this.key, context);
    }
}
