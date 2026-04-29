# Baize Lens

## 🏭 管道设计

Harness Engineering 的核心是将提示词模板能力嵌入任意需要的环节，在任意节点都能直接调用目录内的模板，对专业数据的控制都采用**策略**，策略是对知识库内数据的使用方式描述。

也就是说，可以加载一个提示词模板之后，在模板内加入**策略**，有模型自行根据提问内容筛选对应知识库数据支撑策略执行。

**Psychology Conversation** 的管线设计如下：

```
  Query
    ┃
    ▼
Domain Context + Relation ID
    ┃
    ▼
Matching subtasks ━▶ NO ━▶ Revolve pipline ━━━━━━━━━━━━━┓
    ┃                ┃                                  ┃
    ▼                ┗▶ Prompt template                 ┃
   YES                                                  ┃
    ┃                                                   ┃
    ┣▶ Questionnaire ━▶ start ━▶ process ━▶ stop        ┃
    ┃                                                   ┃
    ┣▶ Guide Flow ━▶ start ━▶ process ━▶ stop           ┃
    ┃                                                   ┃
    ┗▶ Logic rules [BACK]                               ┃
                                                        ┃
                                        ┏━━━━━━━━━━━━━━━┻━━━━┓
                                        ▼                    ▼
                                  Knowledge base       Knowledge base
                                        ┃                    ┃
                                        ▼                    ▼
                                     Report            Prompt template
                                        ┃                    ┃
                                        ▼                    ▼
                                      Answer               Answer
```

**Questionnaire** vs **Guide Flow**

| 功能 | 说明 | 关键能力 |
| ---- | ---- | ---- |
| Questionnaire | 模型根据指定问答模板，依次向用户进行提问，用户围绕提问进行作答 | 🚡  模型遵循指定问答流程的能力 |
| Guide Flow| 模型以互动方式执行向导流，不同的结果引导不同的流程 | 📡 模型按照指定流程（分支）根据具体回答执行不同分支流程 |

### 定义子任务

在 `baize_psychology_subtask.json` 中添加子任务需要引导提示语，按照知识库数据集打包流程自动打包，服务器启动时自动载入。

目前定义的子任务如下表：

| 任务名 | 描述 | 示例 |
| ---- | ---- | ---- |
| `predict_painting` | 对指定的图像文件进行绘画推理 | *"对绘画进行心理学推理分析"* |
| `query_report` | 查询所有报告或者指定报告 | *"显示我的测试"* |
| `select_report` | 选中报告，将报告关联到上下文 | *"选择第1份评测"* |
| `start_questionnaire`| 启动 Questionnaire 流程 | *"用SCL进行评测"* |
| `stop_questionnaire`| 结束 Questionnaire 流程，用于强行终止 | *"结束吧"* |
| `start_guide_flow` | 启动 Guide Flow 流程 | *"诊断一下我的心理健康状态"* |
| `stop_guide_flow` | 结束 Guide Flow 流程，用于强行终止 | *"结束"* |
| `super_admin` | 进入超级管理员模式，可以执行超级管理员指令 | *"大梦"* |
