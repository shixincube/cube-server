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

package cube.service.filestorage.plugin;

import cube.common.entity.AuthDomain;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.auth.AuthPluginContext;
import cube.service.filestorage.FileStorageService;

/**
 * 创建域应用插件。
 */
public class CreateDomainAppPlugin implements Plugin {

    private FileStorageService service;

    public CreateDomainAppPlugin(FileStorageService service) {
        this.service = service;
    }

    @Override
    public void setup() {
    }

    @Override
    public void teardown() {
    }

    @Override
    public HookResult launch(PluginContext context) {
        if (context instanceof AuthPluginContext) {
            AuthPluginContext apc = (AuthPluginContext) context;
            AuthDomain authDomain = apc.getDomain();
            if (null != authDomain) {
                this.service.refreshAuthDomain(authDomain);
            }
        }
        return null;
    }
}
