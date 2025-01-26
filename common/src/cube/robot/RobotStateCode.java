/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.robot;

/**
 * Robot 模块状态码。
 */
public enum RobotStateCode {

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
     * 未授权。
     */
    Unauthorized(12),

    /**
     * 不支持的操作。
     */
    Unsupported(13),

    /**
     * 数据过期。
     */
    Expired(15),

    /**
     * 控制器错误。
     */
    ControllerError(17),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    RobotStateCode(int code) {
        this.code = code;
    }

    public static RobotStateCode parse(int code) {
        for (RobotStateCode stateCode : RobotStateCode.values()) {
            if (stateCode.code == code) {
                return stateCode;
            }
        }

        return RobotStateCode.Unknown;
    }
}
