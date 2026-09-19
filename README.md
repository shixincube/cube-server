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
   客户端 / 浏览器   │  Talk(7000) · WS(7070) · WSS(7077)        │
   移动端 / 第三方   │  HTTP(7010) · HTTPS(7017) · Stream(7171)  │
                    └───────────────────┬───────────────────────┘
                                        │
                     ┌──────────────────▼──────────────────┐
                     │        dispatcher  调度机           │
                     │                                     │
                     │  Performer  接入层连接器            │
                     │    ├── Director 路由表（按 Cellet   │
                     │    │    权重切分区间，加权随机选路） │
                     │    ├── Token → Device → Director     │
                     │    ├── Block   同步转发（轮询等应答）│
                     │    ├── Transmission 异步回送映射     │
                     │    └── StreamServer 大流量数据通道    │
                     │                                     │
                     │  Cellet：Auth / Contact / FileStorage│
                     │    FileProcessor / Messaging / AIGC  │
                     │    CV / Ferry / Hub / Robot / ...    │
                     │                                     │
                     │  Daemon 守护：心跳、超时、日志报告    │
                     └──────────────────┬──────────────────┘
                                        │ Cell Talk 协议（6000）
                     ┌──────────────────▼──────────────────┐
                     │          service  服务单元           │
                     │                                     │
                     │  ServiceCarpet → Kernel → Module     │
                     │    ├── AIGC     模型编排 / 知识库    │
                     │    ├── Auth     域与令牌             │
                     │    ├── Contact  联系人 / 群组 / 会员 │
                     │    ├── FileStorage / FileProcessor   │
                     │    ├── Messaging / MultipointComm    │
                     │    ├── Conference / Hub / Signal     │
                     │    ├── Ferry    摆渡与租约           │
                     │    ├── Robot    机器人任务           │
                     │    ├── CV       视觉计算接口         │
                     │    └── Tokenizer 分词与关键词        │
                     └──────────────────┬──────────────────┘
                                        │
                     ┌──────────────────▼──────────────────┐
                     │  MySQL / 共享内存缓存 / 本地存储      │
                     └─────────────────────────────────────┘
```

| 组件 | 工程目录 | 职责 |
| --- | --- | --- |
| 调度机 | `dispatcher/` | 接入、鉴权、路由、并发控制、REST/WS 接口、流式数据 |
| 服务单元 | `service/` | 业务逻辑、模型编排、知识库、数据持久化 |
| 公共库 | `common/` | 协议包、实体模型、动作枚举、存储与工具 |
| 控制台 | `console/` | Web 管理界面，管理/监视多个服务节点 |
| 应用服务器 | `server-app/` | 独立应用型服务入口 |
| 摆渡服务 | `ferryboat/`、`ferryhouse/` | 跨域消息摆渡与工作区 |
| 扩展服务 | `service-conference/`、`service-messaging/`、`service-multipointcomm/`、`service-filestorage/`、`service-fileprocessor/`、`service-riskmgmt/` | 以 jar 形式动态加载的独立服务单元 |

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
│   │   ├── aigc/                # AIGC 接口管理器 + REST 处理器（97 个）
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
│   │   └── riskmgmt/            # 风控
│   ├── assets/                  # 分享页、App 页、图表、水印等静态资源
│   └── config/dispatcher.properties
│
├── service/                     # 服务单元（业务）
│   ├── src/cube/service/
│   │   ├── ServiceCarpet        # 容器监听器：授权校验、内核装配
│   │   ├── Daemon               # 守护任务：状态报告、日志上报
│   │   ├── Kernel / Module      # 模块容器
│   │   ├── aigc/                # ★ AIGC 核心（240 个源文件）
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
│   ├── config/                  # 服务配置、存储配置、插件声明
│   ├── plugin/                  # 热插拔插件 jar
│   └── lib/                     # 本地动态库
│
├── common/                      # 公共库：协议、实体、动作、存储、工具
├── console/                     # 控制台 Web
├── server-app/                  # 应用服务器
├── ferryboat/ ferryhouse/       # 摆渡服务与工作区
├── deploy/                      # 部署产物与启停脚本
├── build.xml / Makefile         # Ant 构建入口
└── Dockerfile                   # 容器化构建
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

AIGC 相关处理器共 97 个，由 `cube.dispatcher.aigc.Manager` 在启动时统一注册到 Jetty 上下文。主要分组：

| 分组 | 路径示例 | 说明 |
| --- | --- | --- |
| 对话与生成 | `/aigc/chat/`、`/aigc/channel/`、`/aigc/stop/`、`/aigc/cot/`、`/aigc/preinfer/` | 问答、通道、思维链、预推理 |
| NLP | `/aigc/nlp/segmentation`、`/aigc/nlp/semantic`、`/aigc/nlp/summarization` | 分词、语义搜索、摘要 |
| 多模态 | `/multimodal/base/`、`/multimodal/stream/`、`/aigc/text2file/`、`/aigc/facial/expression`、`/aigc/speech/*` | 多模态、文本生成文件、表情、语音识别/情感/说话人分割/谈话分析 |
| 知识库 | `/aigc/knowledge/{new,delete,update,info,doc,import,remove,reset,backup,segment,qa,profile}`、`/aigc/knowledge/article/*` | 知识库生命周期、文档与文章管理 |
| 心理学 | `/aigc/psychology/{check,converse,scale,scales,stop,comprehensive,template}`、`/aigc/psychology/report/*`、`/aigc/psychology/painting`、`/aigc/painting/label` | 量表、绘画、报告与模板文章 |
| 心理咨询策略 | `/aigc/stream/strategy/`、`/aigc/stream/caption/`、`/aigc/copilot/{apply,dispose,sheet}` | 策略查询、副驾（Copilot） |
| 应用层 | `/app/{user,session,verify,activate,membership,config,change,evaluate,inject,keepalive,version}`、`/app/customer/*`、`/app/schedule/*`、`/app/chat`、`/app/wordcloud`、`/app/asciiart`、`/app/emotion` | 移动端应用接口 |
| 运维与文档 | `/aigc/history/`、`/aigc/usage/`、`/aigc/queue/`、`/aigc/event/`、`/doc/api/`、`/static/` | 历史、用量、队列、事件、接口文档 |

鉴权与设备信息通过请求头或路径携带：

| 方式 | 字段 |
| --- | --- |
| 令牌 | 路径最后一段（长度 ≥ 32）、`x-baize-api-token` 头、或 `token` 参数 |
| 设备 | `x-baize-api-device`（设备名）、`x-baize-api-platform`（平台） |
| 其他 | `x-baize-api-client`、`x-baize-api-version` |

其余 REST 分组：`/contact/*`（联系人）、`/cv/*`（条码识别/生成、人形与手势估计、纸张裁切、目标检测）、`/robot/*`（回调注册、账号、执行）、`/ferry/gnosis/`、`/sharing/` 与 `/qrcode/`（文件分享与二维码）。

**② Cell Talk 协议（默认 7000）**

面向长连接的客户端使用二进制协议，通过 `Packet` + `ActionDialect` 承载 `sn` / `name` / `data` / `state`。动作定义集中在 `common/src/cube/common/action/`，其中 `AIGCAction` 共 **124** 个动作常量。

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
```

扩展集群时，只需在 `director.N.*` 追加节点块并调整 `weight`，调度机会自动重算路由区间——**无需重启客户端**。

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

AIGC 是服务单元的核心（240 个源文件），入口是 `AIGCCellet` → `AIGCService`。

**AIGCCellet** 维护 `Responder` 队列：向调度机发出请求后，用 `sn` 匹配应答；`transmit(...)` 默认超时 3 分钟。

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

**场景层**（22 个类）是业务语义的编排中心：

| 类 | 作用 |
| --- | --- |
| `SceneManager` | 通道级会话上下文与量表轨道（`ScaleTrack`）、聊天记录落库 |
| `PsychologyScene` | 心理学场景主控：绘画报告、量表报告、模板文章 |
| `CounselingManager` / `CopilotManager` | 咨询流程与副驾会话管理 |
| `QueryRevolver` / `PromptRevolver` | 查询与提示词的轮转（Revolve）策略 |
| `EvaluationWorker` / `EvaluationWorker`、`ComprehensiveReportWorker`、`ComprehensiveVerifier` | 评测与综合报告生成与校验 |
| `ConversationWorker` / `StreamArchive` / `VoiceDiarizationIndicator` | 对话处理、流归档、说话人指示 |
| `DigitalTwinMachine` | 数字孪生处理 |
| `ContentTools` / `PromptBuilder` / `TemplateArticleBuilder` | 内容与提示词构建 |

**向导流（guidance）** 由 `GuideFlow`（继承 `AbstractGuideFlow`）实现，脚本层用 JS 引擎（Nashorn）加载 `service/assets/guidance/` 下的流程定义，配套 `Guides`、`Prompts` 两个注册表。

**监听器（17 个）** 覆盖 `GenerateText`、`TextToImage`、`TextToFile`、`Summarization`、`SemanticSearch`、`RetrieveReRank`、`AutomaticSpeechRecognition`、`SpeechEmotionRecognition`、`VoiceDiarization`、`FacialExpressionRecognition`、`Multimodal`、`KnowledgeQA`、`KnowledgeProgress`、`ResetKnowledgeStore`、`ReadPage`、`ExtractKeywords` 等异步结果回调。

**插件（7 个）**：`NewFilePlugin`、`DeleteFilePlugin`、`InjectTokenPlugin`、`AppEventPlugin`、`KnowledgeBaseEventPlugin`、`ActivateKnowledgeBasePlugin`、`ContactEventPlugin`，用于把 AIGC 能力挂接到文件、令牌、事件、联系人等系统钩子上。

**资源与检索**：`ResourceSearcher` 抽象 + `BingSearcher` / `BaiduSearcher` 实现（由 `aigc.properties` 的 `page.searcher` 选择）、`FastTokenizer`、`Agent`、`StageDirector`、`AtomCollider`、`AttachmentBuilder`。

**任务层（109 个 Task）** 与 **数据层**（`AIGCStorage`、`LensDataToolkit`、`ReportDataset`、`LensDataset`、`MemberCenter`、`EventCenter`）。

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
| `service/config/aigc.properties` | AIGC 线程池、节点权重、上下文长度（全局与各模型分档）、页面搜索器、代理接口 |
| `service/config/storage*.json` | 各模块的存储后端（默认 MySQL：host / port / schema / user / password） |
| `service/config/psychology.json` | 心理学服务存储与单元配置（`maxQueueLength`、`contextLength` 等） |
| `service/config/plugin.json` | 插件清单：`file`（jar）、`module`、`hooks`（如 `PrePush` → `MessagingPlugin`） |
| `service/config/*-cache.properties` | 共享内存缓存参数（token-pool、general、contact、group、hub、filelabel 等） |
| `service/config/cipher.properties` | 密码机参数 |
| `service/config/robot.properties` | 机器人服务 API 地址与令牌 |

> ⚠️ 仓库中的 `storage*.json` 与 `psychology.json` 含明文数据库账号。生产部署前请改为从环境变量或独立的密钥配置注入，并确认这些文件未被提交到公开仓库。

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

`build` 依次构建 `common` → `dispatcher` → `service` → `ferry` → `console` → `server-app`，产物输出到 `build/` 子目录，`deploy` 将编译结果安装到部署目录。

针对单个工程也可直接构建，如 `ant build-dispatcher-release`、`ant build-service-release`。

### 7.4 启动与停止

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

### 7.5 端口一览

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

### 7.6 容器化

```bash
docker build -t cube-server .
docker run -p 7000:7000 -p 7070:7070 -p 7077:7077 -p 7010:7010 -p 7017:7017 cube-server
```

镜像基于 `cubestack/jdk1.8`，入口为 `cd /home/deploy && ./start.sh && tail -n 100 -f logs/*.out`。

### 7.7 控制台

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

1. 在 `dispatcher/src/cube/dispatcher/aigc/handler/` 新建类，继承 `ContextHandler`，构造函数中 `super("/aigc/<分组>/<名称>")` 并 `setHandler(new Handler())`。
2. 内部 `Handler` 继承 `AIGCHandler`：用 `getApiToken(request)` 取令牌，`Manager.getInstance().checkToken(...)` 校验，非法则返回 401。
3. 通过 `Manager.getInstance().syncRequest(token, AIGCAction.Xxx, data)` 转发到服务单元。
4. 在 `cube.dispatcher.aigc.Manager#setupHandler()` 中 `httpServer.addContextHandler(new Xxx())` 完成注册。

### 8.3 新增提示词模板 / 策略 / 向导流

| 目标 | 操作 |
| --- | --- |
| 提示词模板 | 在 `service/assets/prompt/` 新增 `.md`，并在 `catalog.json` 的 `files` 中登记 `name` 与 `file` |
| 策略 | 在 `service/assets/psychology/strategies/` 新增 `.md`，按人群/场景命名 |
| 向导流 | 在 `service/assets/guidance/` 新增 `.js` + `.json` 配对文件（分支流程）或步骤文案目录 |
| 问卷/量表 | 放入 `service/assets/psychology/questionnaires/` 与 `scale.json` |
| 脚本模组 | 放入 `service/assets/robot/modules/`，由 `ModuleManager` 载入 |

### 8.4 代码约定

- 所有跨节点通信必须携带 `_performer`（P-KEY）；服务单元主动推送必须携带 `_director`（D-KEY）。
- 响应统一携带 `state`：`StateCode.makeState(code, desc)`。
- 服务单元侧的统一应答打包由 `cube.service.Director` 与 `ServiceTask` 完成，勿手工拼接 `Packet`。
- 新增配置项优先写入对应的 `.properties` / `.json`，不要把可变参数硬编码进类。

---

## 许可证

本项目遵循仓库根目录 [LICENSE](LICENSE) 中的条款。

## 获得帮助

- Cube 官网：<https://www.shixincube.com/>
- 邮件：<cube@spap.com>
