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

package cube.service.filestorage;

import cube.common.entity.Contact;
import cube.common.entity.SharingReport;
import cube.common.entity.TraceEvent;

/**
 * 报告数据计算器。
 */
public class SharingReportor {

    private ServiceStorage storage;

    public SharingReportor(ServiceStorage storage) {
        this.storage = storage;
    }

    public SharingReport countRecord(Contact contact) {
        int numTag = this.storage.countSharingTag(contact.getDomain().getName(), contact.getId(), true);
        int numEventView = this.storage.countTraceEvent(contact.getDomain().getName(), contact.getId(),
                TraceEvent.View);
        int numEventExtract = this.storage.countTraceEvent(contact.getDomain().getName(), contact.getId(),
                TraceEvent.Extract);
        int numEventShare = this.storage.countTraceEvent(contact.getDomain().getName(), contact.getId(),
                TraceEvent.Share);

        SharingReport report = new SharingReport(SharingReport.CountRecord);
        report.totalSharingTag = numTag;
        report.totalEventView = numEventView;
        report.totalEventExtract = numEventExtract;
        report.totalEventShare = numEventShare;
        return report;
    }
}
