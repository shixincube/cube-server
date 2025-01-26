/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.action;

/**
 * 授权服务动作。
 */
public enum AuthAction {

    /**
     * 申请令牌。
     */
    ApplyToken("applyToken"),

    /**
     * 获取指定 Code 的令牌。
     */
    GetToken("getToken"),

    /**
     * 获取访问域。
     */
    GetDomain("getDomain"),

    /**
     * 潜伏期。
     */
    Latency("latency"),

    /**
     * 未知动作。
     */
    Unknown("")

    ;

    public final String name;

    AuthAction(String name) {
        this.name = name;
    }
}
