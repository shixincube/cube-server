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

package cube.common.entity;

/**
 * 节点事件。
 */
public final class MonitoringEvent {

    /**
     * 发送。
     */
    public final static String Transmit = "Transmit";

    /**
     * 打开。
     */
    public final static String Open = "Open";

    /**
     * 转发。
     */
    public final static String Forward = "Forward";

    /**
     * 归档。
     */
    public final static String Archive = "Archive";

    /**
     * 删除。
     */
    public final static String Delete = "Delete";

    /**
     * 重命名。
     */
    public final static String Rename = "Rename";

    /**
     * 复制。
     */
    public final static String Copy = "Copy";

    /**
     * 分享。
     */
    public final static String Share = "Share";

    /**
     * 浏览。
     */
    public final static String View = "View";

    /**
     * 浏览已丢失分享文件。
     */
    public final static String ViewLoss = "ViewLoss";

    /**
     * 浏览已过期分享文件。
     */
    public final static String ViewExpired = "ViewExpired";

    /**
     * 提取。
     */
    public final static String Extract = "Extract";

    private MonitoringEvent() {
    }
}
