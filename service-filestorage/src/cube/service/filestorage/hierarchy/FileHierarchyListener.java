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

package cube.service.filestorage.hierarchy;

import cube.common.entity.FileLabel;

/**
 * 文件结构监听器。
 */
public interface FileHierarchyListener {

    /**
     * 当添加文件标签时调用该函数。
     *
     * @param fileHierarchy 文件层级。
     * @param directory 文件目录。
     * @param fileLabel 文件标签。
     */
    public void onFileLabelAdd(FileHierarchy fileHierarchy, Directory directory, FileLabel fileLabel);

    /**
     * 当移除文件标签时调用该函数。
     *
     * @param fileHierarchy 文件层级。
     * @param directory 文件目录。
     * @param fileLabel 文件标签。
     */
    public void onFileLabelRemove(FileHierarchy fileHierarchy, Directory directory, FileLabel fileLabel);

    /**
     * 查询指定的文件标签。
     *
     * @param fileHierarchy
     * @param directory
     * @param uniqueKey
     * @return
     */
    public FileLabel onQueryFileLabel(FileHierarchy fileHierarchy, Directory directory, String uniqueKey);
}
