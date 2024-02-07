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

package cube.core;

import cube.storage.StorageType;
import org.json.JSONObject;

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
     * 获取配置。
     *
     * @return
     */
    public JSONObject getConfig();

    /**
     * 获取类型。
     *
     * @return
     */
    public StorageType getType();

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

    public boolean executeInsert(String table, List<StorageField[]> fieldsList);

    public boolean executeUpdate(String table, StorageField[] fields, Conditional[] conditionals);

    public boolean executeDelete(String table, Conditional[] conditionals);

    public List<StorageField[]> executeQuery(String table, StorageField[] fields);

    public List<StorageField[]> executeQuery(String table, StorageField[] fields, Conditional[] conditionals);

    public List<StorageField[]> executeQuery(String[] tables, StorageField[] fields, Conditional[] conditionals);

    public List<StorageField[]> executeQuery(String sql);

    public boolean execute(String sql);
}
