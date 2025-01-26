/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferry;

/**
 * Ferry 模块状态码。
 */
public enum FerryStateCode {

    /**
     * 成功。
     */
    Ok(0),

    /**
     * 无效参数。
     */
    InvalidParameter(5),

    /**
     * 遇到故障。
     */
    Failure(9),

    /**
     * 无效域信息。
     */
    InvalidDomain(11),

    /**
     * 无效的令牌。
     */
    InvalidToken(12),

    /**
     * 未找到成员。
     */
    NotFindMember(14),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    FerryStateCode(int code) {
        this.code = code;
    }
}
