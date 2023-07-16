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

package cube.aigc.attachment.ui;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 日期选择器组件。
 */
public class DatePicker extends Component {

    public final static String TYPE_DATE = "date";

    public final static String TYPE_DATE_TIME = "datetime";

    public final static String TYPE_DATE_RANGE = "daterange";

    private String type;

    private boolean clearable;

    private JSONArray range;

    public DatePicker(String type) {
        super("DatePicker");
        this.type = type;
        this.clearable = false;
    }

    public void setRange(long start, long end) {
        this.range = new JSONArray();
        this.range.put(start);
        this.range.put(end);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("type", this.type);
        json.put("clearable", this.clearable);

        if (null != this.range) {
            json.put("range", this.range);
        }

        return json;
    }
}
