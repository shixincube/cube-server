/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common;

import org.json.JSONObject;

/**
 * 可转 JSON 结构对象接口。
 */
public interface JSONable {

    /**
     * 序列化为 JSON 格式。
     *
     * @return 返回 JSON 格式表示的数据。
     */
    JSONObject toJSON();

    /**
     * 序列化为简易/紧凑的 JSON 格式。
     *
     * @return 返回 JSON 格式表示的数据。
     */
    JSONObject toCompactJSON();

}
