/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2023 Cube Team.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.ferry;

/**
 * 摆渡服务动作。
 */
public enum FerryAction {

    /**
     * 签入。
     * (H->B)
     */
    CheckIn("checkIn"),

    /**
     * 签出。
     * (H->B)
     */
    CheckOut("checkOut"),

    /**
     * 同步数据。
     * (H->B)
     */
    Synchronize("synchronize"),

    /**
     * House 上线。
     * (S->C)
     */
    Online("online"),

    /**
     * House 下线。
     * (S->C)
     */
    Offline("offline"),

    /**
     * 摆渡数据。
     * (B->H)
     */
    Ferry("ferry"),

    /**
     * 反向操作。
     * (H->B)
     * (S->C)
     */
    Tenet("tenet"),

    /**
     * 取出信条。
     * (C->S)
     */
    TakeOutTenet("takeOutTenet"),

    /**
     * 查询域。
     */
    QueryDomain("queryDomain"),

    /**
     * 加入域。
     */
    JoinDomain("joinDomain"),

    /**
     * 退出域。
     */
    QuitDomain("quitDomain"),

    /**
     * 连通性验证。
     * (C->S->B->H)
     */
    Ping("ping"),

    /**
     * 连通性应答。
     * (H->B->S->C)
     */
    PingAck("pingAck"),

    /**
     * 报告数据。
     * (C-S)
     * (B-H)
     */
    Report("report"),

    /**
     * 报告数据应答。
     * (H->B->S)
     */
    ReportAck("reportAck"),

    /**
     * 标记文件标签。
     * (S->B)
     */
    MarkFile("markFile")

    ;

    public final String name;

    FerryAction(String name) {
        this.name = name;
    }
}
