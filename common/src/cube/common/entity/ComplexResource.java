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

        /**
         * 文件。
         */
        File,

        /**
         * 超链接。
         */
        Hyperlink,

        /**
         * 图表。
         */
        Chart,

        /**
         * 界面组件。
         */
        Widget,

        /**
         * 附录。
         */
        Attachment,

        ;

        public static Subject parse(String name) {
            if (name.equalsIgnoreCase(Subject.File.name())) {
                return File;
            }
            if (name.equalsIgnoreCase(Subject.Hyperlink.name())) {
                return Hyperlink;
            }
            else if (name.equalsIgnoreCase(Subject.Chart.name())) {
                return Chart;
            }
            else if (name.equalsIgnoreCase(Subject.Widget.name())) {
                return Widget;
            }
            else if (name.equalsIgnoreCase(Subject.Attachment.name())) {
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

    protected ComplexResource(Subject subject, JSONObject data) {
        this.subject = subject;
        this.sn = data.has("sn") ? data.getLong("sn") : Utils.generateSerialNumber();
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
