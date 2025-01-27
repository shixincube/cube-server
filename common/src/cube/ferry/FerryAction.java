/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
