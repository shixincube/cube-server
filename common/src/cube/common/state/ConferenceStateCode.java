/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.state;

/**
 * 授权模块状态码。
 */
public enum ConferenceStateCode {

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
     * 未签入联系人。
     */
    NoSignIn(12),

    /**
     * 搜索条件错误。
     */
    SearchConditionError(25),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    ConferenceStateCode(int code) {
        this.code = code;
    }
}
