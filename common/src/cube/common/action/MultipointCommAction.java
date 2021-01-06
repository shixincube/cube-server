/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.common.action;

/**
 * 多方通讯模块的动作定义。
 */
public enum MultipointCommAction {

    /**
     * 信令 Offer
     */
    Offer("offer"),

    /**
     * 信令 Answer
     */
    Answer("answer"),

    /**
     * 信令 Bye
     */
    Bye("bye"),

    /**
     * 信令 Busy
     */
    Busy("busy"),

    /**
     * 信令 Candidate
     */
    Candidate("candidate"),

    /**
     * 应答 Offer
     */
    OfferAck("offerAck"),

    /**
     * 应答 Answer
     */
    AnswerAck("answerAck"),

    /**
     * 应答 Candidate
     */
    CandidateAck("candidateAck"),

    /**
     * 应答 Bye
     */
    ByeAck("byeAck"),

    /**
     * 应答 Busys
     */
    BusyAck("busyAck"),

    /**
     * 执行申请主叫。
     */
    ApplyCall("applyCall"),

    /**
     * 申请进入场域。
     */
    ApplyEnter("applyEnter"),

    /**
     * 执行申请终止。
     */
    ApplyTerminate("applyTerminate"),

    /**
     * 终端节点进入。
     */
    Entered("entered"),

    /**
     * 终端节点退出。
     */
    Left("left"),

    OpenField("open"),

    CloseField("close"),

    /**
     * 未知动作。
     */
    Unknown("")

    ;

    public final String name;

    MultipointCommAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
