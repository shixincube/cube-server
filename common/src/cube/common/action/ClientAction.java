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

    Cube("Cube");

    public final String name;

    ClientAction(String name) {
        this.name = name;
    }
}
