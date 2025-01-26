/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.action;

/**
 * 客户端相关操作动作描述。
 */
public enum ClientAction {

    Login("Login"),

    Logout("Logout"),

    GetLog("GetLog"),

    GetAuthToken("GetAuthToken"),

    InjectAuthToken("InjectAuthToken"),

    AddEventListener("AddEventListener"),

    RemoveEventListener("RemoveEventListener"),

    NotifyEvent("NotifyEvent"),

    GetDomain("GetDomain"),

    CreateDomainApp("CreateDomainApp"),

    UpdateDomain("UpdateDomain"),

    ApplyToken("ApplyToken"),

    ListOnlineContacts("ListOnlineContacts"),

    NewContact("NewContact"),

    GetContact("GetContact"),

    GetGroup("GetGroup"),

    CreateContact("CreateContact"),

    UpdateContact("UpdateContact"),

    PushMessage("PushMessage"),

    QueryMessages("QueryMessages"),

    MarkReadMessages("MarkReadMessages"),

    ModifyContactZone("ModifyContactZone"),

    GetFile("GetFile"),

    PutFile("PutFile"),

    DeleteFile("DeleteFile"),

    FindFile("FindFile"),

    ListFiles("ListFiles"),

    ListSharingTags("ListSharingTags"),

    GetSharingTag("GetSharingTag"),

    ListSharingTraces("ListSharingTraces"),

    TraverseVisitTrace("TraverseVisitTrace"),

    ProcessFile("ProcessFile"),

    SubmitWorkflow("SubmitWorkflow"),

    CancelWorkflow("CancelWorkflow"),

    GetFilePerf("GetFilePerf"),

    UpdateFilePerf("UpdateFilePerf"),

    ListContactBehaviors("ListContactBehaviors"),

    AIGCGetServiceInfo("AIGCGetServiceInfo"),

    Cube("Cube");

    public final String name;

    ClientAction(String name) {
        this.name = name;
    }
}
