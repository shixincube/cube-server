/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.service.aigc.plugin;

import cell.util.log.Logger;
import cube.aigc.AppEvent;
import cube.common.entity.Contact;
import cube.common.entity.FileLabel;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.aigc.AIGCService;

/**
 * 销毁文件。
 */
public class DeleteFilePlugin implements Plugin {

    private final AIGCService service;

    public DeleteFilePlugin(AIGCService service) {
        this.service = service;
    }

    @Override
    public void setup() {
        // Nothing
    }

    @Override
    public void teardown() {
        // Nothing
    }

    @Override
    public HookResult launch(PluginContext context) {
        try {
            FileLabel fileLabel = (FileLabel) context.get("fileLabel");
            Contact contact = (Contact) context.get("contact");

            AppEvent appEvent = new AppEvent(AppEvent.DeleteFile, System.currentTimeMillis(),
                    contact.getId(), fileLabel.toCompactJSON());
            this.service.getStorage().writeAppEvent(appEvent);
        } catch (Exception e) {
            Logger.w(this.getClass(), "#launch", e);
        }

        return null;
    }
}
