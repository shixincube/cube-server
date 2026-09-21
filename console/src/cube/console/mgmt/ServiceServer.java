/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.mgmt;

import cube.common.JSONable;
import cube.console.tool.Detector;
import cube.console.tool.NodeVersion;
import cube.util.NodeName;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 服务单元服务器。
 */
public class ServiceServer implements JSONable {

    public final long timestamp = System.currentTimeMillis();

    public final String tag;

    public final String deployPath;

    public final String configPath;

    public final String celletsPath;

    private boolean local = false;

    private String name;

    private CellConfigFile cellConfigFile;

    private File storageJsonFile;

    private JSONObject storageJson;

    private CacheConfigFile tokenPoolConfigFile;

    private CacheConfigFile generalCacheConfigFile;

    private CacheConfigFile contactCacheConfigFile;

    private CacheConfigFile fileLabelCacheConfigFile;

    private SeriesCacheConfigFile messagingSeriesConfigFile;

    private boolean running = false;

    public ServiceServer(String tag, String deployPath, String configPath, String celletsPath) {
        this.tag = tag;
        this.deployPath = deployPath;
        this.configPath = configPath;
        this.celletsPath = celletsPath;
    }

    public boolean isLocal() {
        return this.local;
    }

    public String getName() {
        return this.name;
    }

    public boolean isRunning() {
        return this.running;
    }

    protected void refresh() {
        try {
            this.cellConfigFile = new CellConfigFile(this.configPath + File.separator + "service.xml");
            // 是否在本地
            this.local = this.cellConfigFile.load();

            if (!this.local) {
                this.name = this.tag + "#service#" + this.configPath;
                return;
            }

            this.name = NodeName.makeName(this.tag, NodeName.ROLE_SERVICE,
                    this.cellConfigFile.getAccessPoint().getPort());

            // 检查是否正在运行
            this.refreshRunning();

            // 检查对应的 Cellet 文件
            List<CellConfigFile.CelletConfig> celletConfigs = this.cellConfigFile.getCelletConfigList();
            for (CellConfigFile.CelletConfig cc : celletConfigs) {
                String jarFilePath = cc.getJarFilePath();
                if (null != jarFilePath) {
                    Path path = Paths.get(this.deployPath, jarFilePath);
                    File file = path.toAbsolutePath().toFile();
                    if (file.exists()) {
                        cc.setJarFile(file);
                    }
                }
            }

            // 存储配置
            this.storageJsonFile = new File(this.configPath + File.separator + "storage_dev.json");
            if (!this.storageJsonFile.exists()) {
                this.storageJsonFile = new File(this.configPath + File.separator + "storage.json");
            }
            byte[] data = Files.readAllBytes(Paths.get(this.storageJsonFile.getAbsolutePath()));
            this.storageJson = new JSONObject(new String(data, Charset.forName("UTF-8")));

            // 令牌缓存池
            this.tokenPoolConfigFile = new CacheConfigFile(this.configPath + File.separator + "token-pool.properties");
            this.tokenPoolConfigFile.load();

            // 通用缓存器
            this.generalCacheConfigFile = new CacheConfigFile(this.configPath + File.separator + "general-cache.properties");
            this.generalCacheConfigFile.load();

            // 联系人缓存器
            this.contactCacheConfigFile = new CacheConfigFile(this.configPath + File.separator + "contact-cache.properties");
            this.contactCacheConfigFile.load();

            // 文件标签缓存器
            this.fileLabelCacheConfigFile = new CacheConfigFile(this.configPath + File.separator + "filelabel-cache.properties");
            this.fileLabelCacheConfigFile.load();

            // 消息时序
            this.messagingSeriesConfigFile = new SeriesCacheConfigFile(this.configPath
                    + File.separator + "messaging-series-memory.properties");
            this.messagingSeriesConfigFile.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 刷新运行状态（不重新加载配置文件，供定时任务高频调用）。
     *
     * 两个信号取或：
     * 1. 控制台在存活窗口内收到过该节点提交的报告 —— 上报即心跳，不要求控制台能反向连上节点；
     * 2. 节点 SHM 访问点端口能从本机建立 TCP 连接 —— 覆盖节点刚启动、首份报告尚未到达的窗口期。
     *
     * 不再依赖 `<deployPath>/bin/tag_service` 这类由启动脚本写入的标记文件：它同时耦合了部署根目录
     * 和 `-tag` 参数名（开发态直接从模块目录启动时标记文件落在模块的 bin 目录、名字也不一定是 service），
     * 一旦不匹配就会把正在运行的节点判定为未运行。
     */
    protected void refreshRunning() {
        if (null == this.cellConfigFile) {
            this.running = false;
            return;
        }

        long now = System.currentTimeMillis();

        if (NodeHeartbeat.getInstance().isAlive(this.name, now)) {
            this.running = true;
            return;
        }

        AccessPoint ap = this.cellConfigFile.getAccessPoint();
        this.running = (null != ap) && Detector.isPortReachable(ap.getHost(), ap.getPort());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("tag", this.tag);
        json.put("deployPath", this.deployPath);
        json.put("configPath", this.configPath);
        json.put("celletsPath", this.celletsPath);
        json.put("name", this.name);
        // 版本号取自部署目录 `libs/` 里 jar 内的 `cube.service.Version`，读不到时置空串
        String version = NodeVersion.read(this.deployPath, NodeVersion.SERVICE);
        json.put("version", null == version ? "" : version);

        json.put("running", this.running);

        json.put("server", this.cellConfigFile.getAccessPoint().toJSON());

        json.put("logLevel", this.cellConfigFile.getLogLevelAsString());

        List<CellConfigFile.CelletConfig> list = this.cellConfigFile.getCelletConfigList();
        JSONArray array = new JSONArray();
        for (CellConfigFile.CelletConfig config : list) {
            array.put(config.toJSON());
        }
        json.put("cellets", array);

        json.put("adapter", this.cellConfigFile.getContactsAdapter().toJSON());

        json.put("storage", this.storageJson);

        json.put("tokenPool", this.tokenPoolConfigFile.toJSON());

        json.put("generalCache", this.generalCacheConfigFile.toJSON());

        json.put("contactCache", this.contactCacheConfigFile.toJSON());

        json.put("fileLabelCache", this.fileLabelCacheConfigFile.toJSON());

        json.put("messagingSeries", this.messagingSeriesConfigFile.toJSON());

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
