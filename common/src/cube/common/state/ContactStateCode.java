/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.state;

/**
 * 联系人模块状态码。
 */
public enum ContactStateCode {

    /**
     * 成功。
     */
    Ok(0),

    /**
     * 无效参数。
     */
    InvalidParameter(5),

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
     * 未签入联系人。
     */
    NoSignIn(12),

    /**
     * 重复签入。
     */
    DuplicateSignIn(13),

    /**
     * 未找到联系人。
     */
    NotFindContact(14),

    /**
     * 未找到群组。
     */
    NotFindGroup(15),

    /**
     * 未找到联系人分区。
     */
    NotFindContactZone(16),

    /**
     * 令牌不一致。
     */
    InconsistentToken(21),

    /**
     * 不被接受的非法操作。
     */
    IllegalOperation(25),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    ContactStateCode(int code) {
        this.code = code;
    }
}
