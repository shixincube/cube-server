/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.file.hook;

import cube.plugin.Hook;

/**
 * 文件存储服务插件钩子。
 */
public class FileStorageHook extends Hook {

    /**
     * 文件保存到系统。
     */
    public final static String SaveFile = "SaveFile";

    /**
     * 从系统里销毁文件。
     */
    public final static String DestroyFile = "DestroyFile";

    /**
     * 目录插入新文件。
     */
    public final static String NewFile = "NewFile";

    /**
     * 删除目录里文件。
     */
    public final static String DeleteFile = "DeleteFile";

    public final static String DownloadFile = "DownloadFile";

    public final static String CreateSharingTag = "CreateSharingTag";

    public final static String Trace = "Trace";

    /**
     * 构造函数。
     *
     * @param key 钩子关键字。
     */
    public FileStorageHook(String key) {
        super(key);
    }
}
