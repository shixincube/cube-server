/**
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

package cube.service.messaging.plugin;

import cell.util.json.JSONException;
import cell.util.log.Logger;
import cube.common.entity.Message;
import cube.plugin.LuaPlugin;
import cube.plugin.PluginContext;

import java.io.FileNotFoundException;

/**
 * 检查消息内容插件。
 */
public class SimplePlugin extends LuaPlugin {

    public SimplePlugin() {
        super("plugins/SimplePlugin.lua");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void onAction(PluginContext context) {
        Logger.i(this.getClass(), "Run lua script: " + this.getLuaFile().getName());

        PluginContext ctx = null;

        Message message = (Message) context.getData();
        if (message.getPayload().has("content")) {
            try {
                ctx = new PluginContext(message.getPayload().getString("content"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            return;
        }

        try {
            this.call(ctx);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // 更新消息内容
        try {
            message.getPayload().put("content", ctx.getResult().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
