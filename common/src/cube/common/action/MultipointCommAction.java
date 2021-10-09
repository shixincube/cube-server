/*
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
     * 邀请进入。
     */
    Invite("invite"),

    /**
     * 终端节点进入。
     */
    Arrived("arrived"),

    /**
     * 终端节点退出。
     */
    Left("left"),

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
     * 邀请应答。
     */
    InviteAck("inviteAck"),

    /**
     * 获取指定通讯场域。
     */
    GetField("getField"),

    /**
     * 创建通讯场域。
     */
    CreateField("createField"),

    /**
     * 销毁通讯场域。
     */
    DestroyField("destroyField"),

    /**
     * 执行申请主叫。
     */
    ApplyCall("applyCall"),

    /**
     * 申请加入场域。
     */
    ApplyJoin("applyJoin"),

    /**
     * 执行申请终止。
     */
    ApplyTerminate("applyTerminate"),

    /**
     * 客户端请求对当期场域进行数据广播。
     */
    Broadcast("broadcast"),

    /**
     * 申请广播数据的服务器应答。
     */
    BroadcastAck("broadcastAck"),

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
