/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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
 * 联系人模块动作定义。
 */
public enum ContactActions {

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
     * 设备超时。
     */
    DeviceTimeout("deviceTimeout"),

    /**
     * 获取联系人信息。
     */
    GetContact("getContact"),

    /**
     * 获取联系人列表。
     */
    GetContactList("getContactList"),

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
    DissolveGroup("dissolveGroup"),

    /**
     * 未知动作。
     */
    Unknown("")

    ;

    public String name;

    ContactActions(String name) {
        this.name = name;
    }
}
