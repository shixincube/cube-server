# Cube Harness Engineering

**Cube Harness Engineering** 是一个面向 AIGC 与实时协作场景的服务端工程。它由**调度机（dispatcher）**与**服务单元（service）**两大核心组件构成，把「提示词模板 + 策略」的编排能力（Harness）嵌入到网关到业务单元的完整链路上：任意环节都可以加载模板、附加策略、调用知识库，并对推理过程进行审计与热部署。

- **调度机（dispatcher）** 是网关层，负责连接接入、鉴权、路由分发、并发控制，并对外暴露 REST/HTTP 与 WebSocket 接口。
- **服务单元（service）** 是业务层，以 Cell 容器 + 内核模块的方式承载 AIGC、联系人、文件、协同、机器人等业务能力。

---

## 目录

- [一、整体架构](#一整体架构)
- [二、工程结构](#二工程结构)
- [三、调度机 dispatcher](#三调度机-dispatcher)
- [四、服务单元 service](#四服务单元-service)
- [五、Harness Engineering 机制](#五harness-engineering-机制)
- [六、配置说明](#六配置说明)
- [七、构建与部署](#七构建与部署)
- [八、开发指引](#八开发指引)

---

## 一、整体架构

```
                    ┌───────────────────────────────────────────┐
   客户端 / 浏览器    │  Talk(7000) · WS(7070) · WSS(7077)        │
   移动端 / 第三方    │  HTTP(7010) · HTTPS(7017) · Stream(7171)  │
                    └───────────────────┬───────────────────────┘
                                        │
                     ┌──────────────────▼───────────────────┐
                     │         dispatcher  调度机            │
                     │                                      │
                     │  Performer  接入层连接器               │
                     │    ├── Director 路由表（按 Cellet      │
                     │    │    权重切分区间，加权随机选路）      │
                     │    ├── Token → Device → Director     │
                     │    ├── Block   同步转发（轮询等应答）    │
                     │    ├── Transmission 异步回送映射        │
                     │    └── StreamServer 大流量数据通道      │
                     │                                       │
                     │  Cellet：Auth / Contact / FileStorage │
                     │    FileProcessor / Messaging / AIGC   │
                     │    CV / Ferry / Hub / Robot / ...     │
                     │                                       │
                     │  Daemon 守护：心跳、超时、日志报告        │
                     └──────────────────┬───────────────────┘
                                        │ Cell Talk 协议（6000）
                     ┌──────────────────▼───────────────────┐
                     │          service  服务单元            │
                     │                                      │
                     │  ServiceCarpet → Kernel → Module     │
                     │    ├── AIGC     模型编排 / 知识库      │
                     │    ├── Auth     域与令牌              │
                     │    ├── Contact  联系人 / 群组 / 会员   │
                     │    ├── FileStorage / FileProcessor   │
                     │    ├── Messaging / MultipointComm    │
                     │    ├── Conference / Hub / Signal     │
                     │    ├── Ferry    摆渡与租约            │
                     │    ├── Robot    机器人任务            │
                     │    ├── CV       视觉计算接口          │
                     │    └── Tokenizer 分词与关键词         │
                     └──────────────────┬──────────────────┘
                                        │
                     ┌──────────────────▼──────────────────┐
                     │   MySQL / 共享内存缓存 / 本地存储      │
                     └─────────────────────────────────────┘
```

| 组件 | 工程目录 | 职责 |
| --- | --- | --- |
| 调度机 | `dispatcher/` | 接入、鉴权、路由、并发控制、REST/WS 接口、流式数据 |
| 服务单元 | `service/` | 业务逻辑、模型编排、知识库、数据持久化 |
| 业务模块 | `service-psychology/` | 心理学业务模块（AIGC Module 形态），以插件 jar 装载，提供 37 个业务动作与 30 个 REST 端点 |
| 公共库 | `common/` | 协议包、实体模型、动作枚举、模块 SPI、存储与工具 |
| 控制台 | `console/` | Web 管理界面，管理/监视多个服务节点 |
| 应用服务器 | `server-app/` | 独立应用型服务入口 |
| 摆渡服务 | `ferryboat/`、`ferryhouse/` | 跨域消息摆渡与工作区 |
| 验证程序 | `verify/` | 模块装载与场景存储的集成验证（全工程唯一同时引用插件与宿主的编译单元） |
| 扩展服务 | `service-conference/`、`service-messaging/`、`service-multipointcomm/`、`service-filestorage/`、`service-fileprocessor/`、`service-riskmgmt/` | 以 jar 形式动态加载的独立服务单元 |

### 业务模块的插件化边界（本仓库的一条硬约束）

`dispatcher/` 与 `service/` 两个工程**不包含任何心理学代码**。心理学业务（含其 REST 端点实现）整体位于 `service-psychology/`，以 AIGC Module 形态装载。依赖方向**单向**：

```
service-psychology  ──编译期──▶  dispatcher   （构造网关端点 handler 所必需）
service-psychology  ──运行期──▶  service      （经 cube.aigc.spi.* 与宿主交互）
dispatcher / service  ──✗──▶  service-psychology   （零编译期引用）
```

- 插件 jar 放入 `deploy/libs/`，与宿主同一 ClassLoader；
- 宿主与网关侧只持有 SPI 接口，按 `module.extensions` 配置的类名**反射装载**，不 import 插件任何类型；
- 模块未装载时，其端点自然不存在（返回 404），宿主不提供任何降级兜底端点。

详见 [5.7 模块 SPI 与端点注入](#57-模块-spi-与端点注入)。

---

## 二、工程结构

```
cube-server/
├── dispatcher/                  # 调度机（网关）
│   ├── src/cube/dispatcher/
│   │   ├── DispatcherListener   # 容器监听器：装载配置、构建 Performer
│   │   ├── Performer            # 接入层连接器：路由、转发、流
│   │   ├── Director / Scope     # 路由节点与作用域（权重）
│   │   ├── DispatcherTask       # 调度任务抽象
│   │   ├── Daemon               # 守护任务：心跳/超时/报告
│   │   ├── stream/              # StreamServer 大流量通道
│   │   ├── aigc/                # AIGC 接口管理器 + 通用 REST 处理器（宿主 63 个端点）
│   │   ├── contact/             # 联系人接口
│   │   ├── filestorage/         # 文件存取与分享页
│   │   ├── fileprocessor/       # 媒体转码与信息隐写
│   │   ├── messaging/           # 即时消息
│   │   ├── multipointcomm/      # 多人实时音视频
│   │   ├── conference/          # 会议
│   │   ├── ferry/               # 摆渡
│   │   ├── hub/                 # 通道与社交通讯
│   │   ├── cv/                  # 视觉计算
│   │   ├── robot/               # 机器人回调
│   │   ├── auth/                # 鉴权
│   │   ├── riskmgmt/            # 风控
│   │   ├── spi/                 # 业务模块 SPI：DispatcherExtension（端点扩展点）+ 装载器
│   │   └── handler/             # 通用 REST 处理器（心理学端点不在此处，见 service-psychology）
│   ├── assets/                  # 分享页、App 页、图表、水印等静态资源
│   └── config/dispatcher.properties
│
├── service-psychology/         # 心理学业务模块（插件 jar，编译期依赖 dispatcher）
│   └── src/cube/service/psychology/
│       ├── PsychologyModule            # 模块入口：描述符、动作绑定、装载与卸载
│       ├── PsychologyStorage           # 模块私有存储（建表在 setup 内一次完成）
│       ├── action/                     # 37 个动作处理器，一动作一类
│       ├── scene/                      # 业务场景：咨询、副驾、语音流、报告生成
│       ├── evaluation/                 # 评测与综合报告
│       ├── dataset/                    # 量表 / 问卷数据集
│       ├── dispatcher/PsychologyEndpoints   # 向网关注入 30 个 REST 端点
│       └── dispatcher/handler/         # 30 个端点实现（继承网关 AIGCHandler）
│
├── service/                     # 服务单元（业务）
│   ├── src/cube/service/
│   │   ├── ServiceCarpet        # 容器监听器：授权校验、内核装配
│   │   ├── Daemon               # 守护任务：状态报告、日志上报
│   │   ├── Kernel / Module      # 模块容器
│   │   ├── aigc/                # ★ AIGC 平台能力（宿主 152 个源文件）
│   │   ├── auth/                # 域、令牌、存储
│   │   ├── contact/             # 联系人、群组、会员、积分统计
│   │   ├── client/              # 服务端内部客户端（与网关互通）
│   │   ├── hub/                 # Hub 服务、通道管理
│   │   ├── ferry/               # 摆渡服务、租约（Tenet）
│   │   ├── robot/               # 机器人引擎与任务
│   │   ├── cv/                  # 视觉计算服务
│   │   ├── signal/              # 信令服务
│   │   └── tokenizer/           # 分词器（词典 + Viterbi + TF-IDF）
│   ├── assets/                  # 提示词模板、策略、向导流、问卷、脚本模组
│   ├── config/                  # 服务配置、存储配置、模块声明
│   ├── plugin/                  # 热插拔插件 jar
│   └── lib/                     # 本地动态库
│
├── verify/                      # 集成验证：模块装载（44 断言）+ 插件契约（162 断言）
│
├── common/                      # 公共库：协议、实体、动作、模块 SPI、存储、工具
├── console/                     # 控制台 Web
├── server-app/                  # 应用服务器
├── ferryboat/ ferryhouse/       # 摆渡服务与工作区
├── deploy/                      # 部署产物与启停脚本
├── build.xml / Makefile         # Ant 构建入口
├── compile-check.sh             # 5 段全量编译验证（common → dispatcher → 插件 → service → verify）
└── Dockerfile                # 容器化构建
```

---

## 三、调度机 dispatcher

调度机是整个集群的**唯一对外入口**。它不承载业务逻辑，只做三件事：**接入、路由、转发**，并按 Cellet 粒度做加权负载均衡。

### 3.1 核心组件

| 类 | 作用 |
| --- | --- |
| `DispatcherListener` | Cell 容器监听器。读取 `config/dispatcher.properties`，注册 Cellet 清单与 `director.N.*` 路由节点，配置 HTTP 服务；在容器初始化时启动 `Performer`，并以 10 秒周期调度 `Daemon` |
| `Performer` | 接入层连接器。持有 Director 路由表、`Token→Device`、`TalkContext→Director`、`Token→Director`、在线联系人、转发记录、阻塞记录等运行时状态；实现 `TalkListener` 接收服务单元回送数据 |
| `Director` | 一个服务节点（导演机）的抽象，含业务 Endpoint、文件 Endpoint、`Scope` 与可发言的 `Speakable` |
| `Scope` | 节点作用域：该节点承载的 Cellet 名称列表 + 权重（1–10，默认 5） |
| `DispatcherTask` | 调度任务抽象，封装 `Packet`/`ActionDialect` 解析与统一的响应打包（`state`/`data`/`code`） |
| `Daemon` | 守护任务：驱动 `onTick`、清理失效会话上下文、清理超时转发记录、生成运维报告与延迟数据 |
| `StreamServer` | 独立端口（默认 7171）的数据流服务器，承载大流量双向数据传输 |

### 3.2 路由与负载均衡

路由表在 `Performer.start()` 中一次性构建为**按权重切分的连续区间**：

```
某 Cellet 的候选节点：  D1(weight=5)      D2(weight=3)      D3(weight=2)
                        ├────────────┤├──────────┤├──────┤
区间（anchor）          0            4 5         7 8      9
                        └──── totalWeight = 10 ────┘
```

- 每个 `Director.Section` 记录 `begin` / `end` / `totalWeight`。
- 选路时在 `[0, totalWeight-1]` 内取随机整数作为 anchor，落入哪个区间就选哪个节点，实现**加权随机**。
- 连接一旦选中节点即写入 `talkDirectorMap` / `tokenDirectorMap`，后续同一会话/令牌保持**粘性路由**，避免跨节点状态漂移。
- 无候选节点时回退到列表首节点；无会话上下文时按**最大权重节点**选取。

### 3.3 转发模型

`Performer` 提供三种转发语义：

| 语义 | 方法 | 说明 |
| --- | --- | --- |
| 异步转发 | `transmit(...)` | 发送后立即返回。基于 `Transmission` 记录 `sn → Cellet + TalkContext`，服务单元回送时按 `sn` 找回原始客户端连接并投递 |
| 同步转发 | `syncTransmit(...)` | 发送后基于 `Block` 阻塞当前线程，以 50ms 间隔轮询应答，默认超时 30s（AIGC 接口管理器使用 90s）。超时返回 `null` 并记录 `Service timeout` |
| 流式转发 | `transmit(..., InputStream)` | 通过 `speakStream` 把输入流以 64KB 分块推送到服务单元，可同时计算 MD5/SHA1 摘要 |

### 3.4 转发标记：P-KEY 与 D-KEY

跨节点通信依赖两个私有参数，二者在 `dispatcher` 与 `service` 两侧成对定义：

- **`_performer`（P-KEY）**：调度机写入，内容为 `{sn, ts}`。服务单元回送应答时原样带回，`Performer.onListened` 依此将数据路由回发起方。
- **`_director`（D-KEY）**：服务单元写入（`cube.service.Director.attachDirector`），内容为 `{id, domain, device?}`。调度机收到后查找在线联系人：未指定 device 则**广播到该联系人的所有在线设备**，指定 device 则**定向投递**。

### 3.5 Cellet 清单

调度机按 Cellet 名称把请求转发给服务单元。`config/dispatcher.properties` 的 `cellets` 项决定开放哪些通道：

```
cellets = Auth, Contact, FileStorage, FileProcessor, Messaging, AIGC, CV, Ferry
```

可选通道还包括 `MultipointComm`、`Conference`、`Hub`、`Robot`。`Client` 通道始终默认开放，用于服务端内部通信。

一个 Cellet 通常由三部分组成：`XxxCellet`（通道实现）+ `PassThroughTask`（默认透传任务）+ `XxxHandler`（REST 处理器，如适用）。

### 3.6 对外接口

**① REST / HTTP（默认 7010 / 7017）**

AIGC 相关端点共 **93** 条，由两条来源装配：

- **宿主端点（63 条）** —— `cube.dispatcher.aigc.Manager#setupHandler()` 内联注册到 Jetty 上下文；
- **业务模块端点（30 条）** —— 由模块插件经 `DispatcherExtension` 提供，网关在**自身端点之后**按 `getContextPath()` 去重注册（先注册者保留，故宿主天然优先）。

主要分组：

| 分组 | 路径示例 | 来源 | 说明 |
| --- | --- | --- | --- |
| 对话与生成 | `/aigc/chat/`、`/aigc/channel/`、`/aigc/stop/`、`/aigc/cot/`、`/aigc/preinfer/` | 部分模块 | 问答、通道、思维链、预推理 |
| NLP | `/aigc/nlp/segmentation`、`/aigc/nlp/semantic`、`/aigc/nlp/summarization` | 宿主 | 分词、语义搜索、摘要 |
| 多模态 | `/multimodal/base/`、`/multimodal/stream/`、`/aigc/text2file/`、`/aigc/facial/expression` | 宿主 | 多模态、文本生成文件、表情 |
| 语音基础能力 | `/aigc/speech/recognition`、`/aigc/speech/diarization`、`/aigc/speech/diarization/opt/`、`/aigc/speech/emotion`、`/aigc/stream/apply/` | 宿主 | 语音识别、说话人分离、语音情绪识别、语音流申请 |
| 知识库 | `/aigc/knowledge/{new,delete,update,info,doc,import,remove,reset,backup,segment,qa,profile}`、`/aigc/knowledge/article/*` | 宿主 | 知识库生命周期、文档与文章管理 |
| 心理学 | `/aigc/psychology/{check,converse,scale,scales,stop,comprehensive,template}`、`/aigc/psychology/report/*`、`/aigc/psychology/painting`、`/aigc/painting/label` | **模块** | 量表、绘画、报告与模板文章 |
| 语音流（咨询） | `/aigc/speech/analysis`、`/aigc/stream/stop/` | **模块** | 语音内容分析、停止语音流（分离与情绪识别由宿主完成） |
| 说话人视图 | `/aigc/chart/`、`/aigc/cot/` | **模块** | 说话人图表与思维链 |
| 心理咨询策略 | `/aigc/stream/strategy/`、`/aigc/stream/caption/`、`/aigc/copilot/{apply,dispose,sheet}` | **模块** | 策略查询、副驾（Copilot） |
| 应用层 | `/app/{user,session,verify,activate,membership,config,change,evaluate,inject,keepalive,version}`、`/app/chat`、`/app/wordcloud`、`/app/asciiart`、`/app/emotion` | 宿主 | 移动端应用接口 |
| 客户与日程 | `/app/customer/*`、`/app/schedule/*` | **模块** | 客户与预约日程 CRUD |
| 运维与文档 | `/aigc/history/`、`/aigc/usage/`、`/aigc/queue/`、`/aigc/event/`、`/doc/api/`、`/static/` | 宿主 | 历史、用量、队列、事件、接口文档 |
| 模块兜底通道 | `/aigc/module/{moduleName}/{actionName}` | 宿主 | 低频动作的透传通道，默认关闭（`module.rest.enabled`） |

鉴权与设备信息通过请求头或路径携带：

| 方式 | 字段 |
| --- | --- |
| 令牌 | 路径最后一段（长度 ≥ 32）、`x-baize-api-token` 头、或 `token` 参数 |
| 设备 | `x-baize-api-device`（设备名）、`x-baize-api-platform`（平台） |
| 其他 | `x-baize-api-client`、`x-baize-api-version` |

其余 REST 分组：`/contact/*`（联系人）、`/cv/*`（条码识别/生成、人形与手势估计、纸张裁切、目标检测）、`/robot/*`（回调注册、账号、执行）、`/ferry/gnosis/`、`/sharing/` 与 `/qrcode/`（文件分享与二维码）。

**② Cell Talk 协议（默认 7000）**

面向长连接的客户端使用二进制协议，通过 `Packet` + `ActionDialect` 承载 `sn` / `name` / `data` / `state`。动作定义集中在 `common/src/cube/common/action/`，其中 `AIGCAction` 共 **123** 个动作常量。

**③ WebSocket / WSS（7070 / 7077）** 与 **Stream（7171）** 分别承载实时双向通信与大流量数据流。

### 3.7 关键配置

`dispatcher/config/dispatcher.properties`：

```properties
# 线程池
threadpool.type=cached
threadpool.max=4

# 并发上限
concurrency.file.in=20
concurrency.file.out=20
concurrency.file.operation=20
concurrency.cv=15

# 开放通道
cellets=Auth,Contact,FileStorage,FileProcessor,Messaging,AIGC,CV,Ferry

# HTTP / HTTPS
http.host=0.0.0.0
http.port=7010
https.host=0.0.0.0
https.port=7017
maxThreads=16
minThreads=4

# 流服务器
stream.port=7171

# 路由节点（编号 1–10）
director.1.address=127.0.0.1
director.1.port=6000
director.1.fs.address=127.0.0.1
director.1.fs.port=6080
director.1.cellets=Auth,Contact,FileStorage,FileProcessor,Messaging,AIGC,CV,Ferry
director.1.weight=5

# 机器人回调
robot.enabled=false
robot.api=http://127.0.0.1:2280/event/callback/{token}
robot.callback=http://127.0.0.1:7010/robot/event/{token}

# 业务模块网关扩展（详见 3.8 与 5.5）
module.extensions=cube.service.psychology.PsychologyDispatcherExtension,cube.service.psychology.dispatcher.PsychologyEndpoints
module.rest.enabled=false
```

扩展集群时，只需在 `director.N.*` 追加节点块并调整 `weight`，调度机会自动重算路由区间——**无需重启客户端**。

⚠️ 插件 jar 必须与 `cube-dispatcher-3.0.jar` 同在 `deploy/libs/`。`ant build-debug` **不含 deploy 阶段**，改动插件后须再跑 `ant deploy`，否则 `Class.forName` 反射失败，端点静默不注册（只记一条 ERROR）。

### 3.8 模块端点注入

网关自身不感知任何业务模块。模块端点由两条配置驱动：

```properties
# 扩展实现类（逗号分隔），按类名反射装载
module.extensions=<前缀声明类>,<端点提供者类>
# 模块动作兜底通道开关
module.rest.enabled=false
```

装载流程（`cube.dispatcher.aigc.spi.DispatcherExtensions`）：

```
读取 module.extensions → Class.forName → 实例化（须实现 DispatcherExtension）
   ↓
收集 getRestPrefixes()  → 前缀冲突检测（先声明者保留，仅用于诊断）
   ↓
宿主 63 条端点注册完毕
   ↓
逐个调用 getEndpointHandlers() → 按 getContextPath() 去重 → 注册
   ├─ 路径重复：记 ERROR 并跳过（宿主优先）
   └─ 单个扩展抛异常：记 ERROR，不影响其余扩展与网关启动
```

`DispatcherExtension` 的两个方法是 **`default` 方法**，只声明前缀的旧实现无需改动即可继续装载。路径一律以各 handler 自身的 `super(path)` 为准，扩展点不参与路径拼接，也不改写任何既有路径字符串——**REST 路径的兼容性由构造器保证，而非由注册框架保证**。

---

## 四、服务单元 service

服务单元是业务逻辑的落点。它基于 **Cell 容器 + Kernel 内核** 的模块化结构，每个 Cellet 在 `install()` 时向内核注册一个模块，模块之间通过 Kernel 相互查找。

### 4.1 生命周期

```
ServiceCarpet.cellPreinitialize   → 创建 Kernel、Daemon，挂载日志句柄
        ↓
ServiceCarpet.cellInitialized     → 校验授权（license/ 证书 + 签名）
                                    │ 失败则终止初始化，服务不可用
                                    ├─ setupKernel()  装载缓存（TokenPool、General）、启动内核、启动密码机
                                    ├─ PluginSystem.load()  加载插件清单
                                    ├─ 每 10 秒调度 Daemon（首次延迟 30 秒）
                                    └─ initManagement()  设置节点名与报告上报地址
        ↓
ServiceCarpet.cellPredestroy      → kernel.dispose()
        ↓
ServiceCarpet.cellDestroyed       → 卸载插件、关闭密码机、卸载缓存、关闭内核
```

`Daemon`（服务侧）负责周期性向控制台上报节点状态与日志。节点名默认由 MAC + `#service#` + 端口生成，可在 `console-follower-service.properties` 中覆盖。

### 4.2 Cellet 清单

`deploy/config/service.xml` 中注册的服务单元：

| Cellet | 类 | 加载方式 |
| --- | --- | --- |
| Client | `cube.service.client.ClientCellet` | 内置 |
| Auth | `cube.service.auth.AuthServiceCellet` | 内置 |
| Contact | `cube.service.contact.ContactServiceCellet` | 内置 |
| FileStorage | `cube.service.filestorage.FileStorageServiceCellet` | 内置 |
| FileProcessor | `cube.service.fileprocessor.FileProcessorServiceCellet` | 内置 |
| Hub | `cube.service.hub.HubCellet` | 内置 |
| Ferry | `cube.service.ferry.FerryCellet` | 内置 |
| Messaging | `cube.service.messaging.MessagingServiceCellet` | jar 动态加载 |
| MultipointComm | `cube.service.multipointcomm.MultipointCommServiceCellet` | jar 动态加载 |
| Conference | `cube.service.conference.ConferenceServiceCellet` | jar 动态加载 |
| RiskMgmt | `cube.service.riskmgmt.RiskManagementCellet` | jar 动态加载 |

> `AIGC`、`CV`、`Robot` 三个服务单元的实现位于 `service/src/cube/service/` 下，部署时按需在 `service.xml` 的 `<cellets>` 中追加注册即可，与调度机侧的 `cellets` 配置保持一致。

### 4.3 业务模块

| 模块 | 关键类 | 能力 |
| --- | --- | --- |
| **auth** | `AuthService`、`AuthStorage`、`AuthDomainFile/Set` | 域管理、令牌签发与校验、基于文件的域配置 |
| **contact** | `ContactManager`、`ContactTable`、`GroupTable`、`MembershipSystem`、`PointSystem`、`StatisticsSystem` | 联系人、设备、群组与区域、点单统计、会员体系；插件化扩展（`CreateDomainAppPlugin`、`FilterContactNamePlugin`） |
| **client** | `ServerClient`、`ClientManager`、`ServerClientHook` | 服务端内部客户端，代表客户端与网关交互：申请令牌、提交文件、处理文件、查询域与群组等 40+ Task |
| **hub** | `HubService`、`WeChatHub`、`ChannelManager`、`SignalController`、`EventController` | 通道化社交与消息接入 |
| **ferry** | `FerryService`、`Tenet`、`TenetManager`、`FerryStorage` | 跨域摆渡与租约管理；`BurnMessagePlugin` / `UpdateMessagePlugin` / `DeleteMessagePlugin` / `SaveFilePlugin` / `WriteMessagePlugin` 插件 |
| **robot** | `RobotService`、`Roboengine`、`AbstractMission`、`WeiXinMessageList`、`DouYinDailyOperation` | 机器人引擎与任务：微信消息处理、抖音日常运营 |
| **cv** | `CVService`、`CVEndpoint` | 视觉能力：姿态/手势估计、目标检测、条码生成与识别、纸张裁切、相似度比对 |
| **tokenizer** | `Tokenizer`、`WordDictionary`、`FinalSeg`、`TFIDFAnalyzer`、`Keyword` | 词典树 + Viterbi 分词，TF-IDF 关键词抽取 |
| **signal** | `SignalService` | 信令服务 |

### 4.4 AIGC 服务单元

AIGC 是服务单元的核心（宿主侧 152 个源文件），入口是 `AIGCCellet` → `AIGCService`。宿主只承载**平台能力**与**模块 SPI 实现**，业务语义（心理学、咨询、副驾、语音流）全部在 `service-psychology/` 内。

**AIGCCellet** 维护 `Responder` 队列：向调度机发出请求后，用 `sn` 匹配应答；`transmit(...)` 默认超时 3 分钟。请求先经 `ActionRouter` 查是否由已装载模块处理，未命中再回退既有的 71 个 `task/` 分支。

**AIGCService**（约 3600 行）提供的能力域：

| 能力域 | 代表方法 |
| --- | --- |
| 模型单元 | `setupUnit` / `teardownUnit` / `getAllUnits` / `selectIdleUnitByName` / `selectUnitBySubtask` |
| 通道 | `createChannel` / `requestChannel` / `getChannel` / `getChannelByToken` |
| 用户与会话 | `createUser` / `modifyUser` / `checkInUser` / `signOutUser` / `getUser` |
| 会员 | `activateMembership` / `cancelMembership` / `newInvitationForToken` |
| 令牌 | `getOrInjectAuthToken` / `getToken` |
| 知识库 | `getKnowledgeFramework` / `getKnowledgeBase` / `getKnowledgeBaseByCategory` |
| 评价与反馈 | `evaluate` |
| 其他 | `createWordCloud`、`getModelConfigs`、`getNotifications`、`getPreference` |

**模型单元（Unit）** 以元数据描述，共 9 类：`GenerateText`、`TextToImage`、`TextToFile`、`SemanticSearch`、`RetrieveReRank`、`Multimodal`、`SpeechRecognition`、`Audio`，外加通用 `UnitMeta`。单元支持按能力（`AICapability`）与子任务（Subtask）双重选择。

**知识库** 由 `KnowledgeFramework` / `KnowledgeBase` / `FrameworkWrapper` 组成，动作面覆盖新建、删除、更新、文档导入/移除、分段查询、激活/释放、重置、备份、文章增删改查与分类——即 Graph RAG 的数据侧骨架。

**场景层**已随心理学业务迁入插件（`service-psychology/src/cube/service/psychology/scene/`，21 个类）：`PsychologyScene`（绘画报告、量表报告、模板文章）、`CounselingManager` / `CopilotManager`（咨询流程与副驾会话）、`VoiceStreamService`（语音流登记与归档）、`StreamArchive`、`EvaluationWorker` 等。宿主 `service/aigc/scene/` 目前为空。

宿主保留的通用构件：`ContentTools` / `PromptBuilder`（内容与提示词构建）、`VoiceDiarizationIndicator`（说话人指标分析）、`UserProfileRenderer`。

**语音基础能力**（语音识别、说话人分离、语音情绪识别）全部在宿主：分离由音频单元执行，结果写入 `voice_diarization` 表；宿主另有「查询/删除分离结果」的入站动作。业务模块经 SPI 只读该结果并回写分析字段。分离结果的**指标分析（内容语料正负面 / 中性）为可选功能**（`performSpeakerDiarization` 的 `sentiment` 参数），关闭时结果中的 `indicator` 保持 `null`。模块侧对**结果收尾**（如把说话人映射为「来访者 / 咨询师」）经 `SpeechModuleListener` 订阅完成，宿主不内置任何业务角色分类。

**向导流（guidance）** 由 `GuideFlow`（继承 `AbstractGuideFlow`）实现，脚本层用 JS 引擎（Nashorn）加载 `service/assets/guidance/` 下的流程定义，配套 `Guides`、`Prompts`、`SkillRegistry` 三个注册表。

**监听器（13 个）** 覆盖 `GenerateText`、`TextToImage`、`TextToFile`、`Summarization`、`SemanticSearch`、`RetrieveReRank`、`AutomaticSpeechRecognition`、`SpeechEmotionRecognition`、`FacialExpressionRecognition`、`Multimodal`、`KnowledgeQA`、`KnowledgeProgress`、`ResetKnowledgeStore`、`ReadPage`、`ExtractKeywords` 等异步结果回调。

**插件（7 个）**：`NewFilePlugin`、`DeleteFilePlugin`、`InjectTokenPlugin`、`AppEventPlugin`、`KnowledgeBaseEventPlugin`、`ActivateKnowledgeBasePlugin`、`ContactEventPlugin`，用于把 AIGC 能力挂接到文件、令牌、事件、联系人等系统钩子上。

**资源与检索**：`ResourceSearcher` 抽象 + `BingSearcher` / `BaiduSearcher` 实现（由 `aigc.properties` 的 `page.searcher` 选择）、`FastTokenizer`、`Relay`、`StageDirector`、`AtomCollider`、`AttachmentBuilder`。

**任务层（71 个 Task）** 与 **数据层**（`AIGCStorage`、`LensDataToolkit`、`ReportDataset`、`MemberCenter`、`EventCenter`）。

**模块 SPI 实现**（`aigc/spi/`，宿主侧）：`AIGCHostImpl`（把宿主能力暴露给模块）、`ActionRunner`（动作骨架：参数校验、令牌解析、异常兜底）、`ModuleRegistry`（按 `aigc-modules.properties` 发现、实例化、绑定动作、调用 `setup`）。装载序为 `enabled → 实例化 → 绑定动作 → setup`，**绑定早于 setup**；`setup` 失败回滚 `unbindAll`；任何模块失败都不阻断宿主启动。

---

## 五、Harness Engineering 机制

Harness 的核心思想：**把提示词模板能力嵌入任意环节**，在任意节点都能直接调用目录内的模板；对专业数据的控制统一采用**策略（strategy）**，策略即「知识库内数据的使用方式描述」。

### 5.1 提示词模板

模板存放于 `service/assets/prompt/`，由 `catalog.json` 索引：

```json
{
  "files": [
    { "name": "general",                 "file": "general.md" },
    { "name": "revolver",                "file": "revolver.md" },
    { "name": "revolver_no_info",        "file": "revolver_no_info.md" },
    { "name": "psy_organize_record",     "file": "psy_organize_record.md" },
    { "name": "psy_supervise_record",    "file": "psy_supervise_record.md" },
    { "name": "psy_template_report",     "file": "psy_template_report.md" },
    { "name": "psy_popularization_report","file": "psy_popularization_report.md" },
    { "name": "psy_appointment_data",    "file": "psy_appointment_data.md" },
    { "name": "psy_appointment_answer",  "file": "psy_appointment_answer.md" },
    { "name": "srbc-integration_zone",   "file": "srbc-integration_zone.md" },
    { "name": "srbc-blind_spots",        "file": "srbc-blind_spots.md" }
  ]
}
```

### 5.2 策略

策略文件位于 `service/assets/psychology/strategies/`，按人群与场景划分，例如：

| 策略 | 适用场景 |
| --- | --- |
| `child_strategy.md` | 儿童 |
| `teenager_strategy.md` | 青少年 |
| `teenager_personality_strategy.md` | 青少年人格特质 |
| `copilot_quick_strategy.md` | 副驾·快速模式 |
| `copilot_deep_strategy.md` | 副驾·深度模式 |
| `yunbao_for_counseling.md` | 云宝咨询 |

策略与模板在代码侧由 `cube.aigc.Prompt` / `StrategyFlow` / `StrategyNode` 承载：`StrategyFlow` 沿节点链依次生成提示词并调用指定的模型单元，直到链尾或失败。

### 5.3 子任务（Subtask）

子任务指具有特定场景的问答机制，用于解决目的指向性明确的问题。在 `AIGCService` 中通过 `selectUnitBySubtask(subtask)` 选择执行单元。典型子任务包括：查询/选择报告、启动/结束/执行各种互动流程、超级管理员模式等。

### 5.4 向导流（Guide Flow）

向导流是**分支式**互动流程：不同回答引导不同分支。定义文件位于 `service/assets/guidance/`，同时提供 `.js`（运行时脚本）与 `.json`（流程结构），例如 `MINI_A3.js` / `.json`、`CareerAssessment.js`、`MiniInternationalNeuropsychiatricInterview.json`，以及 `GuessFamilyName/` 目录下的 `A1.md … B3.md` 步骤文案。

与问卷（Questionnaire）的区别：问卷是**线性**按序提问，向导流是**按分支**推进。

### 5.5 脚本模组与热部署

`service/assets/robot/modules/` 存放以 JS 编写的可热部署模组，例如 `WeiXinMessageTool.js`、`WeiXinIgnoreList.js`、`DouYinVideoInfo.js`、`StopApp.js`。服务侧通过 `ModuleManager` 统一注册、启停模块，`AppManager` 负责应用层模块匹配与语义召回。模组的**热部署**依托三方插件体系（`service/plugin/` 下的 jar）与脚本系统实现，重启后自动载入。

### 5.6 SKILL 技能与提示词编排

**SKILL 技能**是一份可热更新、可跨实例共享的「行为指令」：用 Markdown 描述某类任务的执行规范，平台在需要时把它注入提示词，让模型按既定规范作答。

#### 来源与存储

技能有两个来源，**以存储器（DB 表 `aigc_skill`）为准**，多实例共享同一份定义：

| 来源 | 位置 | 说明 |
| --- | --- | --- |
| 存储器 | MySQL 表 `aigc_skill` | 权威来源，支持热更新，多实例最长在 `skills.cache.ttl` 后生效 |
| 种子目录 | `service/assets/skills/` | 仅用于引导与兜底；可经 `skills.seed` 幂等导入存储器 |

种子目录支持 `<name>/SKILL.md`（或 `skill.md`）与 `<name>.md` 两种形式，以 `_` 或 `.` 开头的目录项被跳过（`_template/` 即模板示例）。`SKILL.md` 支持 YAML 风格的 front-matter：

```markdown
---
name: pdf-report
display_name: PDF 报告生成
description: 根据给定数据生成可打印的 PDF 报告
keywords: pdf, 报告, report
when_to_use: 用户要求导出可打印的报告时
version: 1.0
enabled: true
scope: global
---
技能正文……
```

`scope` 为空或 `global` 表示对所有域生效，否则仅对同名 domain 生效；`enabled: false` 的技能不会参与装载。

#### 三个装载来源（自动引入）

`GenerateTextUnitMeta` 在每次请求时按以下顺序合并出「本次生效的技能集合」，并把它**绑定到会话**：

1. **调用方显式指定**：请求体 `categories` 里的技能名。额外支持两个绑定指令：`*` 清空绑定、`-name` 解绑。
2. **关键词自动装载**：用用户原始请求去匹配技能声明的 `keywords`（分词命中**或**原文包含），上限 `skills.auto.limit`。**只有作者显式声明了 keywords 的技能才会被自动装载**——声明关键词即视为作者的授权，避免误注入。
3. **会话已绑定**：本会话（频道）此前启用过的技能。技能指令**不进入多轮历史**，若不绑定，第二轮起技能就会失效。

绑定以频道为键持久化到 `aigc_skill_session`，因此多实例部署下同一会话在不同实例上仍保持一致；本地另有一层短 TTL 缓存（`skills.session.cache.ttl`）降低读库频率，绑定自身有生存时间（`skills.session.ttl`，默认 30 分钟）。未被识别为技能的 `categories` 名称仍按旧逻辑当作知识释义分类处理。

#### 技能目录注入

当 `skills.catalog=true` 时，提示词里会额外注入一段 `[可用技能]` 目录（技能名 + 描述），让模型知道平台上存在哪些能力。目录段预算很小（`prompt.catalog.ratio`，默认 4%），未用满的额度会顺延给技能段。

#### 提示词编排（PromptComposer）

提示词按语义分段装配，每段有**独立的 Token 预算**，避免某一段超长把其他内容整体挤没：

| 段 | 内容 | 是否可裁剪 |
| --- | --- | --- |
| 系统说明 | 角色与总体约束 | 不参与比例分配 |
| 技能目录 | `[可用技能]` 清单 | 可截断 / 丢弃 |
| 技能指令 | `[技能指令]` + 各技能全文 | 可截断 / 逐条丢弃 |
| 已知信息 | `[已知信息]` + 附件检索结果与知识释义 | 可截断 |
| 用户请求 | `用户请求：…` | **永不丢弃**，超预算时截断并提示 |
| 历史对话 | 仍由协议字段 `history` 承载，由预算反推可携带的条数 | 只保留放得下的最新若干条 |

预算按 **Token** 计量：`上下文窗口 - 输出预留(prompt.output.reserve.ratio)` 得到输入预算，弹性段再按 `prompt.catalog.ratio` / `prompt.skill.ratio` / `prompt.retrieval.ratio` / `prompt.history.ratio` 分配，未用满的额度顺延给后面的段。Token 由 `TokenEstimator` 把字符数换算得到，其「每 Token 字符数」会**用模型单元返回的真实用量（`inputTokens` / `outputTokens`）在线校准**（EWMA）。`prompt.composer=false` 时退化为顺序模式（不做比例切分），用于行为回退。

兼容性：若本次没有任何附加段（无技能、无已知信息、无系统说明），最终提示词与历史版本完全一致，就是用户请求原文。

#### 留痕与排查

| 表 | 内容 |
| --- | --- |
| `aigc_skill_invocation` | 每次涉及技能或发生截断的请求：注入/截断/自动/生效的技能清单、分段预算、估算 Token 与实测 Token、当时校准值，按 `sn` 与对话历史关联 |
| `aigc_skill_session` | 会话级技能绑定（频道、domain、联系人、技能清单、更新时间） |

`#process - Prompt truncated` 日志会在发生截断时输出上下文窗口、输入预算与估算用量，便于回答「模型为什么没看到某个技能」。

#### 相关配置

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `skills.path` | `assets/skills/` | 种子目录，逗号分隔多路径 |
| `skills.cache.ttl` | `60000` | 技能定义缓存刷新间隔（毫秒） |
| `skills.seed` / `skills.seed.overwrite` | `false` / `false` | 启动时是否把种子目录导入存储器、是否覆盖同名 |
| `skills.auto` / `skills.auto.limit` | `true` / `2` | 是否按 keywords 自动装载、单次上限 |
| `skills.catalog` | `true` | 是否注入技能目录 |
| `skills.session` / `skills.session.ttl` / `skills.session.cache.ttl` | `true` / `1800000` / `30000` | 会话绑定的开关、生存时间、本地缓存时间 |
| `prompt.composer` | `true` | 是否启用按比例分段编排 |
| `prompt.output.reserve.ratio` | `25` | 为模型输出预留的上下文比例（%） |
| `prompt.catalog.ratio` / `prompt.skill.ratio` / `prompt.retrieval.ratio` / `prompt.history.ratio` | `4` / `30` / `33` / `33` | 各弹性段预算占比（%） |
| `token.chars.per.token` / `token.calibration.alpha` | `1.8` / `0.3` | Token 估算初值与校准平滑系数 |

> **新增一个技能的步骤**：在 `service/assets/skills/<name>/SKILL.md` 写好带 front-matter 的技能（`enabled: true`，按需声明 `keywords`）→ 若希望随仓库分发并自动入库，置 `skills.seed=true` 后重启服务；否则用 SQL 直接写入 `aigc_skill` 表（或在控制台侧接入管理界面）。发起请求时在 `categories` 里带上技能名即可启用，此后该会话自动沿用。

### 5.7 模块 SPI 与端点注入

业务模块（当前为心理学）以 **AIGC Module** 形态接入：独立工程、独立 jar、运行期装载，可按开关启停。宿主侧不感知任何模块类型，模块侧也不反向依赖宿主实现。

**① 动作面：模块 SPI（`cube.aigc.spi.*`，位于 `common`）**

| 接口 | 作用 |
| --- | --- |
| `ActionModule` | 模块入口：描述符、动作声明、`setup` / `teardown` / `onTick` |
| `ModuleDescriptor` | 模块名、版本、SPI 版本、动作命名空间、REST 前缀、所需单元能力、兄弟依赖、装载超时、是否可选 |
| `ActionBinding` | 单个动作的绑定关系：`requiresToken` 等 |
| `AIGCHost` | 宿主能力门面（存储、文件、模型单元、通道、调度、语音分离结果读写等） |
| `ActionContext` / `AIGCActionTask` | 动作处理上下文与处理器契约 |
| `AIGCSPI` | SPI 契约版本（当前 `VERSION = 25`）。**任何签名变更都必须递增该值**，模块描述符里的 `spiVersion` 与之比对，不匹配即拒绝加载，不做静默降级 |

**② 端点面：网关扩展（`cube.dispatcher.aigc.spi`，位于 `dispatcher`）**

| 类型 | 作用 |
| --- | --- |
| `DispatcherExtension` | 网关扩展点。`getRestPrefixes()` 声明前缀（诊断用），`getEndpointHandlers()`（**default 方法**）返回本模块的 Jetty `ContextHandler` 实例 |
| `DispatcherExtensions` | 装载器：读配置 → 反射实例化 → 前缀冲突检测 → 路径去重注册 |

模块端的对应实现：

| 类 | 作用 |
| --- | --- |
| `PsychologyEndpoints` | 心理学端点清单（30 条），按语义分组，顺序即注册顺序 |
| `psychology/dispatcher/handler/*` | 30 个端点实现，继承网关的 `AIGCHandler`（跨包继承，关键方法为 `protected`，**必须显式 import**） |
| `PsychologyDispatcherExtension` | 纯前缀声明的薄壳，与上面两者并存于同一配置项 |

**③ 编译期依赖是单向的**

```
service-psychology  →  dispatcher   （构造 handler 所必需）
dispatcher          ✗  service-psychology
service             ✗  service-psychology
```

因此：插件 `build.xml` 的 `master-classpath` **包含** `cube-dispatcher-*.jar`；`service-psychology.iml` 需声明 dispatcher 的 COMPILE 作用域依赖；而 `dispatcher/build.xml` 与 `service/build.xml` 刻意**不包含** `cube-service-psychology-*.jar`。

**④ 构建顺序**

插件编译期需要 dispatcher 的产物，故顶层 `build.xml` 的 `build-service-debug` / `build-service-release` 已 `depends` `build-dispatcher-*`：

```
common → dispatcher → service-psychology → service → 其余 service-* → ferry → console → server-app
```

**⑤ 校验**

```bash
./compile-check.sh
```

按 5 段全量编译（common → dispatcher → service-psychology → service → verify），其中 **dispatcher 段必须在插件段之前**，插件段的 classpath 含 dispatcher classes。集成验证在 `/tmp` 下的临时工作目录运行（测试会覆写 `config/` 下的真实配置，**不可在 `service/` 目录直接跑**）：

```bash
mkdir -p /tmp/psy/config && cp service/config/aigc-modules.properties /tmp/psy/config/
# classpath 需含 common / dispatcher / 插件 / service / verify 五套 classes 与依赖 jar
java -cp "$CP" cube.service.aigc.spi.test.ModuleRegistryLoadTest   # 44 断言
java -cp "$CP" cube.service.aigc.spi.test.PsychologyPluginTest     # 162 断言
```

---

## 六、配置说明

| 文件 | 说明 |
| --- | --- |
| `dispatcher/config/dispatcher.properties` | 调度机主配置：线程池、并发上限、开放通道、HTTP/流端口、路由节点、机器人回调 |
| `dispatcher/config/HLSTools.properties` | HLS 流工具参数 |
| `dispatcher/config/console-follower-dispatcher.properties` | 控制台地址与节点名 |
| `deploy/config/dispatcher.xml` | 调度机 Cell 容器配置：监听器、Nucleus（心跳/Talk/WS/WSS/SSL/日志）、Cellet 清单 |
| `deploy/config/service.xml` | 服务单元 Cell 容器配置：监听器、Nucleus、Cellet 清单（含 jar 动态加载项） |
| `service/config/service.properties` | 服务单元线程池（cached / fixed，max） |
| `service/config/aigc.properties` | AIGC 线程池、节点权重、上下文长度（全局与各模型分档）、页面搜索器、代理接口；另含 SKILL 技能（`skills.*`）、提示词编排（`prompt.*`）与 Token 估算（`token.*`）配置，详见 5.6 |
| `service/config/storage*.json` | 各模块的存储后端（默认 MySQL：host / port / schema / user / password） |
| `service/config/aigc-modules.properties` | AIGC 业务模块清单（`module.<n>.class/enabled/optional`），`ModuleRegistry` 据此发现并装载模块；`ant deploy` 会自动把本文件同步到 `deploy/config/`，不要在部署目录另存副本 |
| `dispatcher/config/dispatcher.properties` 的 `module.extensions` | 网关扩展实现类清单（逗号分隔）。同时容纳「只声明前缀」与「提供端点」两类扩展，详见 3.8 与 5.5 |
| `dispatcher/config/dispatcher.properties` 的 `module.rest.enabled` | 模块动作兜底通道 `POST /aigc/module/{moduleName}/{actionName}` 的开关，默认 `false`（关闭时该路径不注册） |
| `service/config/psychology.json.template` | 心理学模块的存储与单元配置模板（`maxQueueLength`、`contextLength` 等），**受版本控制**，只含占位符 |
| `service/config/psychology.local.json` | 心理学模块的实际生效配置，**不受版本控制**（含明文口令）。首次使用时从 `.template` 复制并填入真实值；缺失或仍是占位符时模块拒绝装载并在日志中指明该创建哪个文件 |
| `service/config/plugin.json` | 插件清单：`file`（jar）、`module`、`hooks`（如 `PrePush` → `MessagingPlugin`） |
| `service/config/*-cache.properties` | 共享内存缓存参数（token-pool、general、contact、group、hub、filelabel 等） |
| `service/config/cipher.properties` | 密码机参数 |
| `service/config/robot.properties` | 机器人服务 API 地址与令牌 |

> ⚠️ 仓库中的 `storage*.json` 含明文数据库账号。生产部署前请改为从环境变量或独立的密钥配置注入，并确认这些文件未被提交到公开仓库。
> 心理学模块的数据库配置已按此要求处理：仓库只保留 `psychology.json.template`（占位符），
> 实际配置放在 gitignore 覆盖的 `psychology.local.json`。

---

## 七、构建与部署

### 7.1 环境要求

1. **Java SE 8**（必需）。工程使用 Nashorn（`jdk.nashorn.api.scripting`）执行向导流脚本，JDK 8 是硬性前提。
2. **Apache Ant**（构建入口）：`sudo apt-get install ant` / `yum -y install ant`
3. 可选：Gradle、gcc/make/cmake（用于本地动态库，如 `service/lib/libluajava-1.1.jnilib`）

### 7.2 依赖库

Cube Server 需要与 `cube-server-dependencies` **同级放置**，且不可修改其目录名：

```
cube/
├── cube-server                 # 本仓库
└── cube-server-dependencies    # 依赖库
```

### 7.3 构建

```bash
# 发布构建（默认）
ant build

# Debug 构建
ant build-debug

# 部署到 deploy/ 目录
ant deploy

# 等价 Makefile 入口
make build
make deploy
```

`build` 依次构建 `common` → `dispatcher` → `service-psychology`（业务模块插件）→ `service` → 其余 `service-*` → `ferry` → `console` → `server-app`，产物输出到 `build/` 子目录，`deploy` 将编译结果安装到部署目录。

> ⚠️ `service-psychology` 编译期依赖 `cube-dispatcher-*.jar`（它要构造网关的端点 handler），因此必须排在 `dispatcher` 之后。`build-service-debug` / `build-service-release` 已声明该依赖，但**单独进入该目录手工 `ant` 构建时需自行保证顺序**。
> 插件 jar（`cube-service-psychology-3.0.jar`）由 `deploy` 拷入 `deploy/libs/`，与宿主同一 ClassLoader。**宿主与网关侧编译期均不引用插件**，但运行期网关要靠它装载端点——漏了 `ant deploy` 会导致端点静默消失。

针对单个工程也可直接构建，如 `ant build-dispatcher-release`、`ant build-service-release`。

### 7.4 编译验证

```bash
./compile-check.sh
```

不依赖 Ant，直接用 JDK 8 的 `javac` 做 5 段全量编译（见 5.5 ⑤），用于在改动 SPI、模块或网关端点后快速验证依赖方向与签名一致性。

### 7.5 启动与停止

```bash
cd deploy
./start.sh      # 依次启动 service → dispatcher → ferryboat
./stop.sh

# 单独启动
./start-service.sh
./start-dispatcher.sh
./start-ferryboat.sh
```

默认日志目录为 `deploy/logs/`，可用 `tail -f` 跟踪。

### 7.6 端口一览

| 端口 | 组件 | 协议/用途 |
| --- | --- | --- |
| 6000 | service | Cell Talk 协议（服务单元监听） |
| 6080 | service | 文件端点（`director.N.fs.port`） |
| 7000 | dispatcher | Cell Talk 协议（客户端接入） |
| 7070 | dispatcher | WebSocket |
| 7077 | dispatcher | WebSocket Secure |
| 7010 | dispatcher | HTTP REST |
| 7017 | dispatcher | HTTPS REST |
| 7171 | dispatcher | 数据流（StreamServer） |
| 7080 | console | 控制台 Web |
| 6860 | service | Contacts 适配器 |

### 7.7 容器化

```bash
docker build -t cube-server .
docker run -p 7000:7000 -p 7070:7070 -p 7077:7077 -p 7010:7010 -p 7017:7017 cube-server
```

镜像基于 `cubestack/jdk1.8`，入口为 `cd /home/deploy && ./start.sh && tail -n 100 -f logs/*.out`。

### 7.8 控制台

```bash
cd console
ant build-release   # 需先完成 server 主程序构建
ant start           # 或 nohup ant start &
ant stop
```

默认登录地址 `http://<控制台地址>:7080/`。

---

## 八、开发指引

### 8.1 新增一个服务单元（Cellet）

1. 在 `common/src/cube/common/action/` 中新增动作枚举（参照 `AIGCAction`）。
2. 在 `service/src/cube/service/<module>/` 中实现 `XxxCellet extends AbstractCellet`，在 `install()` 里创建 Service 并通过 `kernel.installModule(NAME, service)` 注册。
3. 在 `dispatcher/src/cube/dispatcher/<module>/` 中实现网关侧 `XxxCellet`，按需提供 `XxxHandler`（REST，若有）。
4. 同步两处配置：
   - `config/dispatcher.properties` 的 `cellets` 与 `director.N.cellets` 加入该名称；
   - `deploy/config/service.xml` 的 `<cellets>` 加入该 Cellet 类。
5. 若需要独立编译单元，参照 `service-messaging/` 的工程结构打成 jar，在 `service.xml` 中以 `jar="cellets/xxx.jar"` 方式加载。

### 8.2 新增一个 REST 接口

先判断归属：**平台能力**（所有模块都能用）与**业务能力**（属某个模块）的做法完全不同。

**① 平台能力端点** —— 加在网关工程：

1. 在 `dispatcher/src/cube/dispatcher/aigc/handler/` 新建类，继承 `ContextHandler`，构造函数中 `super("/aigc/<分组>/<名称>")` 并 `setHandler(new Handler())`。
2. 内部 `Handler` 继承 `AIGCHandler`：用 `getApiToken(request)` 取令牌，`Manager.getInstance().checkToken(...)` 校验，非法则返回 401。
3. 通过 `Manager.getInstance().syncRequest(token, AIGCAction.Xxx, data)` 转发到服务单元。
4. 在 `cube.dispatcher.aigc.Manager#setupHandler()` 中 `httpServer.addContextHandler(new Xxx())` 完成注册。

**② 业务模块端点** —— 加在模块工程，**不要**写进网关：

1. 在 `service-psychology/src/cube/service/psychology/dispatcher/handler/` 新建类（同 ① 的结构，但包名属模块，注意**跨包继承 `AIGCHandler` 必须显式 import**）。
2. 路径写在构造器的 `super(...)` 里，**这是线协议，一经发布不可改**。
3. 在 `PsychologyEndpoints#getEndpointHandlers()` 的列表里登记，顺序即注册顺序。
4. 若同时新增了动作：先在 `common/.../action/AIGCAction.java` 加枚举（字符串即线协议名），再在模块的 `action/` 下实现处理器，并在 `PsychologyModule#declareActionNames()` 与 `getActions()` 中**逐字一致地**声明（不一致会导致请求悬挂而非回错码）。

宿主与网关侧的编译期**不引用**插件任何类型，因此模块改动不需要动网关代码——只需重启网关让端点重新装载。

### 8.3 新增一个业务模块

1. 新建工程目录（如 `service-inspection/`），拷贝 `service-psychology/build.xml` 与 `.iml` 作为骨架。
2. 实现 `ActionModule`（`getName` / `getDescriptor` / `declareActionNames` / `getActions` / `setup` / `teardown` / `onTick`）。`declareActionNames()` 必须与 `getActions()` 逐字一致。
3. 如需 REST 端点，实现 `cube.dispatcher.aigc.spi.DispatcherExtension`，把端点清单与实现放进模块的 `dispatcher/` 子包。
4. 在 `deploy/libs/` 放置模块 jar（与宿主同 ClassLoader），在 `service/config/aigc-modules.properties` 声明 `module.<n>.class` 与 `enabled`。
5. 在 `dispatcher/config/dispatcher.properties` 的 `module.extensions` 追加实现类全限定名。
6. 构建顺序：`dispatcher` 之后（模块编译期依赖它）。
7. 用 `./compile-check.sh` 验证，并在 `verify/` 下补集成验证（该模块是全工程唯一允许同时引用插件与宿主的编译单元）。

### 8.4 新增提示词模板 / 策略 / 向导流

| 目标 | 操作 |
| --- | --- |
| 提示词模板 | 在 `service/assets/prompt/` 新增 `.md`，并在 `catalog.json` 的 `files` 中登记 `name` 与 `file` |
| 策略 | 在 `service/assets/psychology/strategies/` 新增 `.md`，按人群/场景命名 |
| 向导流 | 在 `service/assets/guidance/` 新增 `.js` + `.json` 配对文件（分支流程）或步骤文案目录 |
| 问卷/量表 | 放入 `service/assets/psychology/questionnaires/` 与 `scale.json` |
| 脚本模组 | 放入 `service/assets/robot/modules/`，由 `ModuleManager` 载入 |

### 8.5 代码约定

- 所有跨节点通信必须携带 `_performer`（P-KEY）；服务单元主动推送必须携带 `_director`（D-KEY）。
- 响应统一携带 `state`：`StateCode.makeState(code, desc)`。
- 服务单元侧的统一应答打包由 `cube.service.Director` 与 `ServiceTask` 完成，勿手工拼接 `Packet`。
- 新增配置项优先写入对应的 `.properties` / `.json`，不要把可变参数硬编码进类。

---

## 许可证

本项目遵循仓库根目录 [LICENSE](LICENSE) 中的条款。

## 获得帮助

- Cube 官网：<https://www.aimindecho.com/>
- 邮件：<cube@aimindecho.com>
