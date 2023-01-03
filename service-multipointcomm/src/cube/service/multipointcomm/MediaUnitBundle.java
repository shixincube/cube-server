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
