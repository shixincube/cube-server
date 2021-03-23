/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 会议邀请信息。
 */
public class Invitation implements JSONable {

    /**
     * 被邀请人。
     */
    protected String invitee;

    /**
     * 被邀请人显示的名称。
     */
    protected String displayName;

    /**
     * 是否接受邀请。
     */
    protected boolean accepted = false;

    /**
     * 接受邀请时间。
     */
    protected boolean acceptionTime;

    /**
     * 构造函数。
     *
     * @param invitee
     * @param displayName
     */
    public Invitation(String invitee, String displayName) {
        this.invitee = invitee;
        this.displayName = displayName;
    }

    public Invitation(JSONObject json) {

    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("invitee", this.invitee);
        json.put("displayName", this.displayName);
        json.put("accepted", this.accepted);
        json.put("acceptionTime", this.acceptionTime);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
