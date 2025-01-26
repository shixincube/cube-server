/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub;

/**
 * Hub 模块状态码。
 */
public enum HubStateCode {

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
     * 不支持的信令。
     */
    UnsupportedSignal(13),

    /**
     * 不支持的事件。
     */
    UnsupportedEvent(14),

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

    HubStateCode(int code) {
        this.code = code;
    }

    public static HubStateCode parse(int code) {
        for (HubStateCode stateCode : HubStateCode.values()) {
            if (stateCode.code == code) {
                return stateCode;
            }
        }

        return HubStateCode.Unknown;
    }
}
