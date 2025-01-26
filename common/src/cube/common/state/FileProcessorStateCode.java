/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.state;

/**
 * 文件处理状态码。
 */
public enum FileProcessorStateCode {

    /**
     * 成功。
     */
    Ok(0),

    /**
     * 未授权的请求。
     */
    Unauthorized(4),

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
     * 操作超时。
     */
    OptTimeout(12),

    /**
     * 无文件。
     */
    NoFile(14),

    /**
     * 文件数据异常。
     */
    FileDataException(15),

    /**
     * 与 CV 服务器无连接。
     */
    NoCVConnection(21),

    /**
     * 未知的状态。
     */
    Unknown(99)

    ;

    public final int code;

    FileProcessorStateCode(int code) {
        this.code = code;
    }

    public static FileProcessorStateCode parse(int code) {
        for (FileProcessorStateCode sc : FileProcessorStateCode.values()) {
            if (sc.code == code) {
                return sc;
            }
        }
        return FileProcessorStateCode.Unknown;
    }
}
