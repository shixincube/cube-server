/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.account;

/**
 * 登录状态码。
 */
public enum StateCode {

    /**
     * 成功。
     */
    Success(0),

    /**
     * 不被允许的行为。
     */
    NotAllowed(1),

    /**
     * 找不到用户。
     */
    NotFindAccount(5),

    /**
     * 无效的令牌。
     */
    InvalidToken(6),

    /**
     * 找不到令牌。
     */
    NotFindToken(7),

    /**
     * 无效账号。
     */
    InvalidAccount(8),

    /**
     * 查询数据错误。
     */
    DataError(9),

    /**
     * 无效验证码。
     */
    InvalidCaptcha(11),

    /**
     * 其他状态。
     */
    Other(99);

    /**
     * 编码。
     */
    public final int code;

    StateCode(int code) {
        this.code = code;
    }
}
