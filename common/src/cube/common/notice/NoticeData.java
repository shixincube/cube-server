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

package cube.common.notice;

import org.json.JSONObject;

/**
 * 通知数据。
 */
public class NoticeData extends JSONObject {

    public final static String ACTION = "action";

    public final static String ASYNC_NOTIFIER = "_async_notifier";

    public final static String CONTACT_ID = "contactId";

    public final static String DOMAIN = "domain";

    public final static String PARAMETER = "parameter";

    public final static String CODE = "code";

    public final static String DATA = "data";

    public final static String FILE_CODE = "fileCode";

    public NoticeData(String action) {
        super();
        this.put(NoticeData.ACTION, action);
    }

    public NoticeData(JSONObject json, String... names) {
        super(json, names);
        this.put(NoticeData.ACTION, json.getString(NoticeData.ACTION));
    }

    public String getAction() {
        return this.getString(NoticeData.ACTION);
    }
}
