/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.action;

public enum FileStorageAction {

    /**
     * 相关配置参数。
     */
    Performance("performance"),

    /**
     * 更新配置参数。
     */
    UpdatePerformance("updatePerformance"),

    /**
     * 上传文件。
     */
    UploadFile("uploadFile"),

    /**
     * 放置文件。
     */
    PutFile("putFile"),

    /**
     * 获取文件。
     */
    GetFile("getFile"),

    /**
     * 列表符合条件的文件。
     */
    ListFileLabels("listFileLabels"),

    /**
     * 加载文件数据。
     */
    LoadFile("loadFile"),

    /**
     * 保存本地文件到系统。
     */
    SaveFile("saveFile"),

    /**
     * 获取根目录信息。
     */
    GetRoot("getRoot"),

    /**
     * 列表目录清单。
     */
    ListDirs("listDirs"),

    /**
     * 列表文件清单。
     */
    ListFiles("listFiles"),

    /**
     * 创建新目录。
     */
    NewDir("newDir"),

    /**
     * 删除目录。
     */
    DeleteDir("deleteDir"),

    /**
     * 重命名目录。
     */
    RenameDir("renameDir"),

    /**
     * 插入文件到目录。
     */
    InsertFile("insertFile"),

    /**
     * 移动文件到其他目录。
     */
    MoveFile("moveFile"),

    /**
     * 文件重命名。
     */
    RenameFile("renameFile"),

    /**
     * 删除文件。
     */
    DeleteFile("deleteFile"),

    /**
     * 罗列回收站里的废弃数据。
     */
    ListTrash("listTrash"),

    /**
     * 抹除回收站里的废弃数据。
     */
    EraseTrash("eraseTrash"),

    /**
     * 清空回收站里的废弃数据。
     */
    EmptyTrash("emptyTrash"),

    /**
     * 从回收站恢复废弃数据。
     */
    RestoreTrash("restoreTrash"),

    /**
     * 检索文件。
     */
    SearchFile("searchFile"),

    /**
     * 精确查找文件。
     */
    FindFile("findFile"),

    /**
     * 清空所有数据。
     */
    Cleanup("cleanup"),

    /**
     * 创建分享标签。
     */
    CreateSharingTag("createSharingTag"),

    /**
     * 取消分享标签。
     */
    CancelSharingTag("cancelSharingTag"),

    /**
     * 删除分享标签。
     */
    DeleteSharingTag("deleteSharingTag"),

    /**
     * 获取分享标签。
     */
    GetSharingTag("getSharingTag"),

    /**
     * 获取分享报告数据。
     */
    GetSharingReport("getSharingReport"),

    /**
     * 列表当前联系人的所有分享标签。
     */
    ListSharingTags("listSharingTags"),

    /**
     * 分享页面监测数据。
     */
    Trace("trace"),

    /**
     * 获取监测数据。
     */
    ListSharingTraces("listSharingTraces"),

    /**
     * 未知动作。
     */
    Unknown("")

    ;

    public final String name;

    FileStorageAction(String name) {
        this.name = name;
    }
}
