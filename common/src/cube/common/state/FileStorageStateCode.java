/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.state;

/**
 * 文件存储模块状态码。
 */
public enum FileStorageStateCode {

    /**
     * 成功。
     */
    Ok(0),

    /**
     * 无效参数。
     */
    InvalidParameter(5),

    /**
     * 系统忙。
     */
    Busy(8),

    /**
     * 遇到故障。
     */
    Failure(9),

    /**
     * 无效域信息。
     */
    InvalidDomain(11),

    /**
     * 无效的参数，禁止访问。
     */
    Forbidden(12),

    /**
     * 未找到指定数据。
     */
    NotFound(13),

    /**
     * 未授权访问。
     */
    Unauthorized(14),

    /**
     * 拒绝操作。
     */
    Reject(15),

    /**
     * 文件标签错误。
     */
    FileLabelError(16),

    /**
     * 正在写入文件。
     */
    Writing(17),

    /**
     * 没有目录。
     */
    NoDirectory(18),

    /**
     * 重名。
     */
    DuplicationOfName(20),

    /**
     * 数据过期。
     */
    DataExpired(21),

    /**
     * 存储空间溢出。
     */
    SpaceSizeOverflow(23),

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

    FileStorageStateCode(int code) {
        this.code = code;
    }
}
