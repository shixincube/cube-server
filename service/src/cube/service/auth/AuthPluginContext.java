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

package cube.service.auth;

import cube.auth.AuthToken;
import cube.common.entity.AuthDomain;
import cube.plugin.PluginContext;

/**
 * 插件上下文。
 */
public class AuthPluginContext extends PluginContext {

    private AuthDomain domain;

    private AuthToken token;

    public AuthPluginContext(AuthDomain domain) {
        super();
        this.domain = domain;
    }

    public AuthPluginContext(AuthToken token) {
        super();
        this.token = token;
    }

    public AuthDomain getDomain() {
        return this.domain;
    }

    public AuthToken getToken() {
        return this.token;
    }

    @Override
    public Object get(String name) {
        if (name.equals("domain")) {
            return this.domain;
        }
        else if (name.equals("token")) {
            return this.token;
        }

        return null;
    }

    @Override
    public void set(String name, Object value) {
        if (name.equals("domain")) {
            this.domain = (AuthDomain) value;
        }
        else if (name.equals("token")) {
            this.token = (AuthToken) value;
        }
    }
}
