/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.state;

/**
 * 消息模块状态码。
 */
public enum MessagingStateCode {

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
     * 数据结构错误。
     */
    DataStructureError(12),

    /**
     * 没有域信息。
     */
    NoDomain(13),

    /**
     * 没有设备信息。
     */
    NoDevice(14),

    /**
     * 没有找到联系人。
     */
    NoContact(15),

    /**
     * 没有找到群组。
     */
    NoGroup(16),

    /**
     * 附件错误。
     */
    AttachmentError(17),

    /**
     * 群组错误。
     */
    GroupError(18),

    /**
     * 数据丢失。
     */
    DataLost(20),

    /**
     * 被对方阻止。
     */
    BeBlocked(30),

    /**
     * 敏感数据。
     */
    SensitiveData(31),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    MessagingStateCode(int code) {
        this.code = code;
    }
}
