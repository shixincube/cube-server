/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.mq;

import cell.adapter.CelletAdapter;
import cube.core.AbstractMQ;
import cube.core.MQMessage;
import cube.core.MQTopic;
import org.json.JSONObject;

/**
 * 基于适配器实现的 MQ 。
 */
public class AdapterMQ extends AbstractMQ {

    public final static String TYPE = "AMQ";

    private String name;

    private JSONObject config;

    private CelletAdapter adapter;

    public AdapterMQ(String name) {
        super(name);
    }

    @Override
    public void configure(JSONObject config) {
        super.configure(config);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void publish(MQTopic topic, MQMessage json) {

    }

    @Override
    public void subscribe(MQTopic topic) {

    }
}
