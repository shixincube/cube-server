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
