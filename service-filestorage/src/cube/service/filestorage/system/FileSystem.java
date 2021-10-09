/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
     * @param file
     * @return
     */
    public FileDescriptor writeFile(String fileName, File file);

    /**
     * 写入文件。
     *
     * @param fileName
     * @param inputStream
     * @return
     */
    public FileDescriptor writeFile(String fileName, InputStream inputStream);

    /**
     * 指定名称的文件是否正在写入。
     *
     * @param fileName
     * @return
     */
    public boolean isWriting(String fileName);

    /**
     * 删除文件。
     *
     * @param fileName
     */
    public void deleteFile(String fileName);

    /**
     * 文件是否存在。
     *
     * @param fileName
     * @return
     */
    public boolean existsFile(String fileName);

    /**
     * 将文件读取到磁盘。
     *
     * @param fileName
     * @return 返回文件所在的磁盘目录。
     */
    public String loadFileToDisk(String fileName);

}
