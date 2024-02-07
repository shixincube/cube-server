/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.robot;

/**
 * Robot 动作。
 */
public enum RobotAction {

    /**
     * 触发事件。
     */
    Event("event"),

    /**
     * 注册监听器。
     */
    RegisterListener("registerListener"),

    /**
     * 注销监听器。
     */
    DeregisterListener("deregisterListener"),

    /**
     * 获取在线设备列表。
     */
    GetOnlineList("getOnlineList"),

    /**
     * 获取账号列表。
     */
    GetAccountList("getAccountList"),

    /**
     * 获取账号数据。
     */
    GetAccount("getAccount"),

    /**
     * 执行任务。
     */
    Perform("perform"),

    /**
     * 取消任务。
     */
    Cancel("cancel"),

    /**
     * 获取报告文件。
     */
    GetReportFile("getReportFile"),

    /**
     * 获取所有脚本文件路径。
     */
    ListScriptFiles("listScriptFiles"),

    /**
     * 下载脚本文件。
     */
    DownloadScriptFile("downloadScriptFile"),

    /**
     * 上传脚本文件。
     */
    UploadScriptFile("uploadScriptFile")

    ;

    public final String name;

    RobotAction(String name) {
        this.name = name;
    }
}
