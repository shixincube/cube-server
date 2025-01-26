/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferry;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;

/**
 * 摆渡数据包。
 */
public class FerryPacket {

    public final static String MESSAGING = "Messaging";

    private long sn;

    private ActionDialect dialect;

    private String domain;

    public FerryPacket(ActionDialect dialect) {
        this.sn = Utils.generateSerialNumber();
        this.dialect = dialect;
    }

    public FerryPacket(String portName) {
        this.sn = Utils.generateSerialNumber();
        this.dialect = new ActionDialect(FerryAction.Ferry.name);
        this.dialect.addParam("port", portName);
    }

    public long getSN() {
        return this.sn;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return this.domain;
    }

    public ActionDialect getDialect() {
        return this.dialect;
    }

    public ActionDialect toDialect() {
        if (!this.dialect.containsParam("sn")) {
            this.dialect.addParam("sn", this.sn);
        }

        if (null != this.domain) {
            this.dialect.addParam("domain", this.domain);
        }

        return this.dialect;
    }
}
