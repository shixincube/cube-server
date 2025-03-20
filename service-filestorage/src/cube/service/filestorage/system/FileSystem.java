/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage.system;

import java.io.File;
import java.io.InputStream;

/**
 * 文件系统接口。
 */
public interface FileSystem {

    /**
     * 启动。
     */
    public void start();

    /**
     * 停止。
     */
    public void stop();

    /**
     * 写入文件。
     *
     * @param fileName 指定文件名。
     * @param fileCode 指定文件码。
     * @param file
     * @return
     */
    public FileDescriptor writeFile(String fileName, String fileCode, File file);

    /**
     * 写入文件。
     *
     * @param fileName 指定文件名。
     * @param fileCode 指定文件码。
     * @param inputStream
     * @return
     */
    public FileDescriptor writeFile(String fileName, String fileCode, InputStream inputStream);

    /**
     * 指定文件码的文件是否正在写入。
     *
     * @param fileCode
     * @return
     */
    public boolean isWriting(String fileCode);

    /**
     * 删除文件。
     *
     * @param fileCode
     * @return
     */
    public boolean deleteFile(String fileCode);

    /**
     * 文件是否存在。
     *
     * @param fileCode
     * @return
     */
    public boolean existsFile(String fileCode);

    /**
     * 将文件读取到磁盘。
     *
     * @param fileCode
     * @return 返回文件所在的磁盘目录。
     */
    public String loadFileToDisk(String fileCode);

}
