/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.state;

/**
 * AIGC 模块状态码。
 */
public enum AIGCStateCode {

    /**
     * 成功。
     */
    Ok(0),

    /**
     * 无效参数。
     */
    InvalidParameter(5),

    /**
     * 未找到指定数据。
     */
    NotFound(6),

    /**
     * 正在处理数据。
     */
    Processing(7),

    /**
     * 数据结构错误。
     */
    DataStructureError(8),

    /**
     * 遇到故障。
     */
    Failure(9),

    /**
     * 无效域信息。
     */
    InvalidDomain(11),

    /**
     * 被中断的操作。
     */
    Interrupted(12),

    /**
     * 令牌不一致。
     */
    InconsistentToken(21),

    /**
     * 没有令牌。
     */
    NoToken(22),

    /**
     * 无有效数据。
     */
    NoData(23),

    /**
     * 无效数据。
     */
    InvalidData(24),

    /**
     * 不被接受的非法操作。
     */
    IllegalOperation(25),

    /**
     * 系统忙。
     */
    Busy(26),

    /**
     * 已停止的动作。
     */
    Stopped(27),

    /**
     * 单元未就绪。
     */
    UnitNoReady(30),

    /**
     * 单元处理错误。
     */
    UnitError(31),

    /**
     * 文件处理错误。
     */
    FileError(32),

    /**
     * 内容长度越界。
     */
    ContentLengthOverflow(41),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    AIGCStateCode(int code) {
        this.code = code;
    }

    public static AIGCStateCode parse(int code) {
        for (AIGCStateCode sc : AIGCStateCode.values()) {
            if (sc.code == code) {
                return sc;
            }
        }
        return Unknown;
    }
}
