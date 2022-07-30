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

package cube.common.notice;

import org.json.JSONObject;

/**
 * 计算分享标签。
 */
public class CountSharingTags extends NoticeData {

    public final static String ACTION = "CountSharingTags";

    public final static String DOMAIN = "domain";

    public final static String CONTACT_ID = "contactId";

    public final static String VALID_NUMBER = "valid";

    public final static String INVALID_NUMBER = "invalid";

    public CountSharingTags(String domain, long contactId) {
        super(ACTION);
        this.put(DOMAIN, domain);
        this.put(CONTACT_ID, contactId);
    }

    public static int parseValidNumber(JSONObject json) {
        return json.has(VALID_NUMBER) ? json.getInt(VALID_NUMBER) : 0;
    }

    public static int parseInvalidNumber(JSONObject json) {
        return json.has(INVALID_NUMBER) ? json.getInt(INVALID_NUMBER) : 0;
    }
}
