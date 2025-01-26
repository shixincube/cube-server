/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.auth;

import cube.plugin.Hook;

/**
 * 授权服务插件的 Hook 。
 */
public class AuthServiceHook extends Hook {

    public final static String CreateDomainApp = "CreateDomainApp";

    public final static String InjectToken = "InjectToken";

    public AuthServiceHook(String key) {
        super(key);
    }
}
