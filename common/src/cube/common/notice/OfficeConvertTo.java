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

import cube.common.action.FileProcessorAction;
import org.json.JSONObject;

/**
 * 文档转换。
 */
public class OfficeConvertTo extends NoticeData {

    public final static String ACTION = FileProcessorAction.OfficeConvertTo.name;

    public final static String DOMAIN = "domain";

    public final static String FILE_CODE = "fileCode";

    public final static String PARAMETER = "parameter";

    public final static String FORMAT = "format";

    public OfficeConvertTo(String domain, String fileCode, String format) {
        super(ACTION);
        this.put(DOMAIN, domain);
        this.put(FILE_CODE, fileCode);

        JSONObject parameter = new JSONObject();
        parameter.put(FORMAT, format);

        this.put(PARAMETER, parameter);
    }
}
