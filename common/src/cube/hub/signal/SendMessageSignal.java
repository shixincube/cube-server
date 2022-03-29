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

package cube.hub.signal;

import cell.util.Base64;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 发送消息信令。
 */
public class SendMessageSignal extends Signal {

    public final static String NAME = "SendMessage";

    private Contact account;

    private Contact partner;

    private Group group;

    private String text;

    public SendMessageSignal(String channelCode, Contact account, Contact partner, String text) {
        super(NAME);
        setCode(channelCode);
        this.account = account;
        this.partner = partner;
        this.text = text;
    }

    public SendMessageSignal(String channelCode, Contact account, Group group, String text) {
        super(NAME);
        setCode(channelCode);
        this.account = account;
        this.group = group;
        this.text = text;
    }

    public SendMessageSignal(JSONObject json) {
        super(json);

        this.account = new Contact(json.getJSONObject("account"));

        if (json.has("partner")) {
            this.partner = new Contact(json.getJSONObject("partner"));
        }

        if (json.has("group")) {
            this.group = new Group(json.getJSONObject("group"));
        }

        try {
            byte[] bytes = Base64.decode(json.getString("text"));
            this.text = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Contact getAccount() {
        return this.account;
    }

    public Contact getPartner() {
        return this.partner;
    }

    public Group getGroup() {
        return this.group;
    }

    public String getText() {
        return this.text;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();

        json.put("account", this.account.toCompactJSON());

        if (null != this.partner) {
            json.put("partner", this.partner.toCompactJSON());
        }

        if (null != this.group) {
            json.put("group", this.group.toCompactJSON());
        }

        String base64 = Base64.encodeBytes(this.text.getBytes(StandardCharsets.UTF_8));
        json.put("text", base64);
        return json;
    }
}
