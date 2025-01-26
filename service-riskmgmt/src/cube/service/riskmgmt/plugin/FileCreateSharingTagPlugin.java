/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.riskmgmt.plugin;

import cube.common.entity.SharingTag;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.filestorage.FileStoragePluginContext;
import cube.service.riskmgmt.RiskManagement;

/**
 * 创建文件分享标签插件。
 */
public class FileCreateSharingTagPlugin implements Plugin {

    private RiskManagement service;

    public FileCreateSharingTagPlugin(RiskManagement service) {
        this.service = service;
    }

    @Override
    public void setup() {
    }

    @Override
    public void teardown() {
    }

    @Override
    public HookResult launch(PluginContext context) {
        FileStoragePluginContext ctx = (FileStoragePluginContext) context;
        SharingTag sharingTag = ctx.getSharingTag();
        this.service.addFileChainNode(sharingTag);

        return null;
    }
}
