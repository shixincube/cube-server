/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
