/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
