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

package cube.core;

import cell.util.json.JSONObject;

import java.util.List;

/**
 * 存储器接口。
 */
public interface Storage {

    /**
     * 获取存储器名称。
     *
     * @return
     */
    public String getName();

    /**
     *
     * @return
     */
    public JSONObject getConfig();

    /**
     *
     * @param config
     */
    public void configure(JSONObject config);

    /**
     * 打开存储仓库。
     */
    public void open();

    /**
     * 关闭存储仓库。
     */
    public void close();

    public boolean exist(String table);

    public boolean executeCreate(String table, StorageField[] fields);

    public boolean executeInsert(String table, StorageField[] fields);

    public List<StorageField[]> executeQuery(String table, StorageField[] fields);

    public List<StorageField[]> executeQuery(String table, StorageField[] fields, Conditional[] conditionals);

}
