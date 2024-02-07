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

package cube.common.entity;

import cell.util.Utils;
import cube.common.JSONable;
import org.json.JSONObject;

/**
 * 复合资源基类。
 */
public abstract class ComplexResource implements JSONable {

    public enum Subject {

        Hyperlink,

        Chart,

        Attachment,

        ;

        public static Subject parse(String name) {
            if (name.equals(Subject.Hyperlink.name())) {
                return Hyperlink;
            }
            else if (name.equals(Subject.Chart.name())) {
                return Chart;
            }
            else if (name.equals(Subject.Attachment.name())) {
                return Attachment;
            }
            else {
                return null;
            }
        }
    }

    protected final Subject subject;

    public final long sn;

    protected ComplexResource(Subject subject) {
        this.subject = subject;
        this.sn = Utils.generateSerialNumber();
    }

    protected ComplexResource(Subject subject, long sn) {
        this.subject = subject;
        this.sn = sn;
    }

    protected ComplexResource(Subject subject, JSONObject json) {
        this.subject = subject;
        this.sn = json.has("sn") ? json.getLong("sn") : Utils.generateSerialNumber();
    }

    public Subject getSubject() {
        return this.subject;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("subject", this.subject.name());
        json.put("sn", this.sn);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("subject", this.subject.name());
        json.put("sn", this.sn);
        return json;
    }
}
