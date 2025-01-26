/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.action;

/**
 * 会议服务动作。
 */
public enum ConferenceAction {

    /**
     * 查询指定条件的会议。
     */
    ListConferences("listConferences"),

    /**
     * 创建会议。
     */
    CreateConference("createConference"),

    /**
     * 接受会议邀请。
     */
    AcceptInvitation("acceptInvitation"),

    /**
     * 拒绝会议邀请。
     */
    DeclineInvitation("declineInvitation"),

    /**
     * 清空所有数据。
     */
    Cleanup("cleanup"),

    /**
     * 未知动作。
     */
    Unknown("")

    ;

    public final String name;

    ConferenceAction(String name) {
        this.name = name;
    }
}
