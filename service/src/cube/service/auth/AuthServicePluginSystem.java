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

package cube.service.auth;

import cube.plugin.PluginSystem;

/**
 * Auth 服务插件系统。
 */
public class AuthServicePluginSystem extends PluginSystem<AuthServiceHook> {

    public AuthServicePluginSystem() {
        super();
        this.build();
    }

    public AuthServiceHook getCreateDomainAppHook() {
        return this.getHook(AuthServiceHook.CreateDomainApp);
    }

    public AuthServiceHook getInjectTokenHook() {
        return this.getHook(AuthServiceHook.InjectToken);
    }

    private void build() {
        AuthServiceHook hook = new AuthServiceHook(AuthServiceHook.CreateDomainApp);
        this.addHook(hook);

        hook = new AuthServiceHook(AuthServiceHook.InjectToken);
        this.addHook(hook);
    }
}
