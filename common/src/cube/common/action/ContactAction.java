/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.action;

/**
 * 联系人模块动作定义。
 */
public enum ContactAction {

    /**
     * 终端签入。
     */
    SignIn("signIn"),

    /**
     * 终端签出。
     */
    SignOut("signOut"),

    /**
     * 恢复终端当前连接。
     */
    Comeback("comeback"),

    /**
     * 申请当前联系人的所有终端下线。
     */
    Leave("leave"),

    /**
     * 设备断线。
     */
    Disconnect("disconnect"),

    /**
     * 设备超时。
     */
    DeviceTimeout("deviceTimeout"),

    /**
     * 获取联系人信息。
     */
    GetContact("getContact"),

    /**
     * 修改联系人信息。
     */
    ModifyContact("modifyContact"),

    /**
     * 新建联系人。
     */
    NewContact("newContact"),

    /**
     * 删除联系人。
     */
    DeleteContact("deleteContact"),

    /**
     * 按照指定参数列出联系人分区清单。
     */
    ListContactZones("listContactZones"),

    /**
     * 获取联系人分区。
     */
    GetContactZone("getContactZone"),

    /**
     * 创建联系人分区。
     */
    CreateContactZone("createContactZone"),

    /**
     * 删除联系人分区。
     */
    DeleteContactZone("deleteContactZone"),

    /**
     * 指定分区是否包含指定参与人。
     */
    ContainsParticipantInZone("containsParticipantInZone"),

    /**
     * 添加参与人到分区。
     */
    AddParticipantToZone("addParticipantToZone"),

    /**
     * 从分区移除参与人。
     */
    RemoveParticipantFromZone("removeParticipantFromZone"),

    /**
     * 修改分区参与人信息。
     */
    ModifyZoneParticipant("modifyZoneParticipant"),

    /**
     * 修改分区信息，有参与人加入或移除。
     */
    ModifyZone("modifyZone"),

    /**
     * 获取群组信息。
     */
    GetGroup("getGroup"),

    /**
     * 按照指定参数列出群组清单。
     */
    ListGroups("listGroups"),

    /**
     * 创建群组。
     */
    CreateGroup("createGroup"),

    /**
     * 解散群组。
     */
    DismissGroup("dismissGroup"),

    /**
     * 向群组添加成员。
     */
    AddGroupMember("addGroupMember"),

    /**
     * 从群组移除成员。
     */
    RemoveGroupMember("removeGroupMember"),

    /**
     * 修改群组信息。
     */
    ModifyGroup("modifyGroup"),

    /**
     * 获取指定的附录。
     */
    GetAppendix("getAppendix"),

    /**
     * 更新附录。
     */
    UpdateAppendix("updateAppendix"),

    /**
     * 群组的附录已更新。
     */
    GroupAppendixUpdated("groupAppendixUpdated"),

    /**
     * 置顶操作。
     */
    TopList("topList"),

    /**
     * 阻止清单操作。
     */
    BlockList("blockList"),

    /**
     * 搜索联系人或群组。
     */
    Search("search"),

    /**人
     * 申请验证码。
     */
    RequestVerificationCode("requestVerificationCode"),

    /**
     * 核验验证码。
     */
    VerifyVerificationCode("verifyVerificationCode"),

    /**
     * 激活会员。
     */
    ActivateMembership("activateMembership"),

    /**
     * 撤销会员。
     */
    CancelMembership("cancelMembership"),

    /**
     * 未知动作。
     */
    Unknown("")

    ;

    public final String name;

    ContactAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
