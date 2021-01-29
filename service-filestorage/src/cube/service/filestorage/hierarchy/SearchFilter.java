/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

import cube.util.FileType;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索过滤器。
 */
public class SearchFilter {

    protected List<FileType> fileTypes = new ArrayList<>();

    protected boolean inverseOrder = true;

    protected int beginIndex = 0;

    protected int endIndex = 20;

    private int hash = 0;

    public SearchFilter(JSONObject json) {

    }

    /**
     * 判断指定的文件类型是否包含在搜索条件里。
     *
     * @param fileType
     * @return
     */
    public boolean containsFileType(FileType fileType) {
        return this.fileTypes.contains(fileType);
    }

    @Override
    public boolean equals(Object object) {
        if (null == object || !(object instanceof SearchFilter)) {
            return false;
        }

        SearchFilter other = (SearchFilter) object;
        if (inverseOrder != other.inverseOrder || beginIndex != other.beginIndex
                || endIndex != other.endIndex || fileTypes.size() != other.fileTypes.size()) {
            return false;
        }

        for (FileType type : fileTypes) {
            if (!other.fileTypes.contains(type)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        if (0 == this.hash) {
            StringBuilder buf = new StringBuilder();
            for (FileType fileType : this.fileTypes) {
                buf.append(fileType.getExtensions()[0]);
            }
            buf.append(this.inverseOrder);
            buf.append(this.beginIndex);
            buf.append(this.endIndex);
            this.hash = buf.toString().hashCode();
        }

        return this.hash;
    }
}
