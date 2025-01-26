/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
