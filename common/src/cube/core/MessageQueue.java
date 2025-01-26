/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import org.json.JSONObject;

/**
 * 消息队列接口。
 */
public interface MessageQueue {

    /**
     * 返回缓存名称。
     *
     * @return 返回缓存名称。
     */
    public String getName();

    /**
     * 返回配置数据。
     *
     * @return 返回配置数据。
     */
    public JSONObject getConfig();

    public void configure(JSONObject config);

    public void start();

    public void stop();

    public void publish(MQTopic topic, MQMessage json);

    public void subscribe(MQTopic topic);

}
