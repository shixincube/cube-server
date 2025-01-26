/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.robot;

import cube.robot.Account;
import cube.robot.Schedule;
import cube.robot.Task;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Roboengine 接口 API 。
 */
public interface Roboengine {

    /**
     * 服务器是否在线。
     *
     * @return
     */
    boolean isServerOnline();

    /**
     * 获取任务。
     *
     * @param taskName
     * @return
     */
    Task getTask(String taskName);

    /**
     * 创建任务。
     *
     * @param taskName
     * @param timeInMillis
     * @param timeFlag
     * @param mainFile
     * @param taskFile
     * @return
     */
    Task createTask(String taskName, long timeInMillis, int timeFlag, String mainFile, String taskFile);

    /**
     * 查询任务计划表。
     *
     * @param accountId
     * @param taskId
     * @return
     */
    Schedule querySchedule(long accountId, long taskId);

    /**
     * 创建新计划表。
     *
     * @param taskId
     * @param accountId
     * @param releaseTime
     * @return
     */
    Schedule newSchedule(long taskId, long accountId, long releaseTime);

    /**
     * 立即推送计划表给对应的设备。
     *
     * @param schedule
     * @return
     */
    boolean pushSchedule(Schedule schedule);

    /**
     * 取消推送计划表，如果任务正在执行，则立即结束。
     *
     * @param schedule
     * @return
     */
    boolean cancelSchedule(Schedule schedule);

    /**
     * 获取在线账号列表。
     *
     * @return
     */
    List<Account> getOnlineAccounts();

    /**
     * 批量获取账号列表。
     *
     * @param begin
     * @param end
     * @return
     */
    List<Account> getAccountList(int begin, int end);

    /**
     * 获取账号数据。
     *
     * @param accountId 账号 ID 。
     * @return
     */
    Account getAccount(long accountId);

    /**
     * 上传脚本。
     *
     * @param filename 文件名。
     * @param file 文件。
     * @return
     */
    boolean uploadScript(String filename, Path file);

    /**
     * 下载报告文件。
     *
     * @param filename
     * @param outputStream
     * @return
     */
    boolean downloadReportFile(String filename, OutputStream outputStream);
}
