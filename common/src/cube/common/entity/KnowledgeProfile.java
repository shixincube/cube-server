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

import org.json.JSONObject;

/**
 * 知识库的侧写。
 */
public class KnowledgeProfile extends Entity {

    public final static int STATE_NORMAL = 0;

    public final static int STATE_READ = 1;

    public final static int STATE_FORBIDDEN = 2;

    public final long contactId;

    public final int state;

    /**
     * 最大可用空间。
     */
    public final long maxSize;

    public final KnowledgeScope scope;

    public KnowledgeProfile(long id, long contactId, String domain, int state, long maxSize, KnowledgeScope scope) {
        super(id, domain);
        this.contactId = contactId;
        this.state = state;
        this.maxSize = maxSize;
        this.scope = scope;
    }

    public KnowledgeProfile(JSONObject json) {
        super(json);
        this.contactId = json.getLong("contactId");
        this.state = json.getInt("state");
        this.maxSize = json.getLong("maxSize");
        this.scope = KnowledgeScope.parse(json.getString("scope"));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("contactId", this.contactId);
        json.put("state", this.state);
        json.put("maxSize", this.maxSize);
        json.put("scope", this.scope.name);
        return json;
    }

    public static KnowledgeProfile createDummy() {
        return new KnowledgeProfile(0, 0, "shixincube.com", STATE_FORBIDDEN, 0,
                KnowledgeScope.Private);
    }
}
