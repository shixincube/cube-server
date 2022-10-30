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

import cube.common.action.FileStorageAction;
import cube.util.ConfigUtils;

/**
 * 批量获取分享标签。
 */
public class ListSharingTags extends NoticeData {

    public final static String ACTION = FileStorageAction.ListSharingTags.name;

    public final static String BEGIN = "begin";

    public final static String END = "end";

    public final static String BEGIN_TIME = "beginTime";

    public final static String END_TIME = "endTime";

    public final static String VALID = "valid";

    public final static String ORDER = "order";

    public ListSharingTags(long contactId, String domain, int begin, int end, boolean valid) {
        super(ListSharingTags.ACTION);
        this.put(CONTACT_ID, contactId);
        this.put(DOMAIN, domain);
        this.put(BEGIN, begin);
        this.put(END, end);
        this.put(VALID, valid);
        this.put(ORDER, ConfigUtils.ORDER_DESC);
    }

    public ListSharingTags(long contactId, String domain, long beginTime, long endTime, boolean valid) {
        super(ListSharingTags.ACTION);
        this.put(CONTACT_ID, contactId);
        this.put(DOMAIN, domain);
        this.put(BEGIN_TIME, beginTime);
        this.put(END_TIME, endTime);
        this.put(VALID, valid);
        this.put(ORDER, ConfigUtils.ORDER_DESC);
    }
}
