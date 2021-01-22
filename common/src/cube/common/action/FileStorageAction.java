/**
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

package cube.common.action;

public enum FileStorageAction {

    /**
     * 上传文件。
     */
    UploadFile("uploadFile"),

    /**
     * 放置文件。
     */
    PutFile("putFile"),

    /**
     * 获取文件。
     */
    GetFile("getFile"),

    /**
     * 获取根目录信息。
     */
    GetRoot("getRoot"),

    /**
     * 罗列目录清单。
     */
    ListDirs("listDirs"),

    /**
     * 罗列文件清单。
     */
    ListFiles("listFiles"),

    /**
     * 创建新目录。
     */
    NewDir("newDir"),

    /**
     * 删除目录。
     */
    DeleteDir("deleteDir"),

    /**
     * 未知动作。
     */
    Unknown("")

    ;

    public final String name;

    FileStorageAction(String name) {
        this.name = name;
    }
}
