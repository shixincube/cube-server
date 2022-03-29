/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.hub.event;

import cell.util.Base64;
import cube.common.entity.Contact;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 发送消息事件。
 */
public class SendMessageEvent extends WeChatEvent {

    public final static String NAME = "SendMessage";

    private String text;

    public SendMessageEvent(Contact account, String text) {
        super(NAME, account);
        this.text = text;
    }

    public SendMessageEvent(JSONObject json) {
        super(json);
        try {
            byte[] bytes = Base64.decode(json.getString("text"));
            this.text = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getText() {
        return this.text;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        String base64 = Base64.encodeBytes(this.text.getBytes(StandardCharsets.UTF_8));
        json.put("text", base64);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        String base64 = Base64.encodeBytes(this.text.getBytes(StandardCharsets.UTF_8));
        json.put("text", base64);
        return json;
    }
}
