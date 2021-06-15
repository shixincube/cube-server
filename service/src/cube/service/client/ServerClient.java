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

package cube.service.client;

import cell.core.talk.TalkContext;
import cube.common.entity.Entity;
import org.json.JSONObject;

/**
 * 客户端管理实体。
 */
public class ServerClient extends Entity {

    private Long id;

    protected TalkContext talkContext;

    public ServerClient(Long id, TalkContext talkContext) {
        this.id = id;
        this.talkContext = talkContext;
    }

    public void setTalkContext(TalkContext talkContext) {
        this.talkContext = talkContext;
    }

    public TalkContext getTalkContext() {
        return this.talkContext;
    }

    public void disable() {

    }

    @Override
    public JSONObject toJSON() {
        return null;
    }

    @Override
    public JSONObject toCompactJSON() {
        return null;
    }
}
