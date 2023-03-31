/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.common.entity;

import cell.core.talk.TalkContext;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AIGC 服务单元。
 */
public class AIGCUnit extends Entity {

    private String key;

    private Contact contact;

    private AICapability capability;

    private TalkContext context;

    private AtomicBoolean running;

    public AIGCUnit(Contact contact, AICapability capability, TalkContext context) {
        super(contact.id, contact.domain.getName());
        this.key = AIGCUnit.makeQueryKey(contact, capability);
        this.contact = contact;
        this.capability = capability;
        this.context = context;
        this.running = new AtomicBoolean(false);
    }

    public String getQueryKey() {
        return this.key;
    }

    public Contact getContact() {
        return this.contact;
    }

    public AICapability getCapability() {
        return this.capability;
    }

    public TalkContext getContext() {
        return this.context;
    }

    public void setTalkContext(TalkContext context) {
        this.context = context;
    }

    public void setRunning(boolean value) {
        this.running.set(value);
    }

    public boolean isRunning() {
        return this.running.get();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("capability", this.capability.toJSON());
        return json;
    }

    public static String makeQueryKey(Contact contact, AICapability capability) {
        StringBuilder buf = new StringBuilder(contact.getId().toString());
        buf.append("_");
        buf.append(contact.domain.getName());
        buf.append("_");
        buf.append(capability.getName());
        buf.append("_");
        buf.append(capability.getSubtask());
        return buf.toString();
    }
}
