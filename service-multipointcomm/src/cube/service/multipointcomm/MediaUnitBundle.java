/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cube.common.entity.CommField;

/**
 * 媒体单元对应关系。
 */
public class MediaUnitBundle {

    protected final AbstractForwardingMediaUnit forwardingMediaUnit;

    protected final AbstractCompositeMediaUnit compositeMediaUnit;

    protected final CommField commField;

    public MediaUnitBundle(AbstractForwardingMediaUnit mediaUnit, CommField commField) {
        this.forwardingMediaUnit = mediaUnit;
        this.commField = commField;
        this.compositeMediaUnit = null;
    }

    public MediaUnitBundle(AbstractCompositeMediaUnit mediaUnit, CommField commField) {
        this.compositeMediaUnit = mediaUnit;
        this.commField = commField;
        this.forwardingMediaUnit = null;
    }

    public MediaUnit getMediaUnit() {
        return (null != this.forwardingMediaUnit) ? this.forwardingMediaUnit : this.compositeMediaUnit;
    }
}
