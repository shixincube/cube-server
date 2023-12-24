package cube.service.riskmgmt.plugin;

import cube.common.entity.ContactBehavior;
import cube.file.hook.FileStorageHook;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.filestorage.FileStoragePluginContext;
import cube.service.riskmgmt.RiskManagement;
import org.json.JSONObject;

/**
 * 文件存储服务事件插件。
 */
public class FileStoragePlugin implements Plugin {

    private final RiskManagement service;

    public FileStoragePlugin(RiskManagement service) {
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
        if (context instanceof FileStoragePluginContext) {
            FileStoragePluginContext ctx = (FileStoragePluginContext) context;
            if (ctx.getKey().equals(FileStorageHook.DownloadFile)) {
                ContactBehavior behavior = new ContactBehavior(ctx.getContact(), ContactBehavior.DOWNLOAD_FILE);
                // 设备
                behavior.setDevice(ctx.getDevice());
                // 参数
                JSONObject parameter = new JSONObject();
                parameter.put("file", ctx.getFileLabel().toCompactJSON());
                behavior.setParameter(parameter);
                // 添加记录
                this.service.recordContactBehavior(behavior);
            }
            else if (ctx.getKey().equals(FileStorageHook.NewFile)) {
                ContactBehavior behavior = new ContactBehavior(ctx.getContact(), ContactBehavior.NEW_FILE);
                behavior.setDevice(ctx.getDevice());
                JSONObject parameter = new JSONObject();
                parameter.put("file", ctx.getFileLabel().toCompactJSON());
                parameter.put("directory", ctx.getDirectory().toCompactJSON());
                behavior.setParameter(parameter);
                // 添加记录
                this.service.recordContactBehavior(behavior);
            }
            else if (ctx.getKey().equals(FileStorageHook.DeleteFile)) {
                ContactBehavior behavior = new ContactBehavior(ctx.getContact(), ContactBehavior.DELETE_FILE);
                behavior.setDevice(ctx.getDevice());
                JSONObject parameter = new JSONObject();
                parameter.put("file", ctx.getFileLabel().toCompactJSON());
                parameter.put("directory", ctx.getDirectory().toCompactJSON());
                behavior.setParameter(parameter);
                // 添加记录
                this.service.recordContactBehavior(behavior);
            }
        }

        return null;
    }
}
