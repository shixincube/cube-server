# Baize Lens

## 🏭 管道设计

Harness Engineering 的核心是将提示词模板能力嵌入任意需要的环节，在任意节点都能直接调用目录内的模板，对专业数据的控制都采用**策略**，策略是对知识库内数据的使用方式描述。

也就是说，可以加载一个提示词模板之后，在模板内加入**策略**，有模型自行根据提问内容筛选对应知识库数据支撑策略执行。

**Conversation** 的管线设计如下：

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
    ┣▶ Appointment ━▶ start ━▶ process ━▶ stop          ┃
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

> [!NOTE]
>
> 利用语义知识图谱机制尽可能利用上下文在一次推理即获得结果。即能一次推理完成的任务不使用 Flow 进行多步推理，同时通过附加 `strategy` 策略指定推理的最终目标。

**Questionnaire** *vs* **Guide Flow**

| 功能 | 说明 | 关键能力 |
| ---- | ---- | ---- |
| Questionnaire | 模型根据指定问答模板，依次向用户进行提问，用户围绕提问进行作答 | 🚡  模型遵循指定问答流程的能力 |
| Guide Flow| 模型以互动方式执行向导流，不同的结果引导不同的流程 | 📡 模型按照指定流程（分支）根据具体回答执行不同分支流程 |

## 子任务（Subtask）

子任务是指具有特定场景的问答机制，一般用于解决具体的、目的指向性明确的问题，例如：根据 DSM 规范对行为人进行诊断，这个过程 Baize 围绕这个诊断主题持续进行推理。

目前爱心理平台和白泽云宝平台的相关场景功能都采用子任务方式进行实现，例如：推理房树人绘画、查询报告、进行互动量表问答等。

在 `baize_psychology_subtask.json` 中添加子任务需要引导提示语，执行打包脚本，按照知识库数据集打包流程自动打包，服务器启动时自动载入。

```bash
# build dataset.json
cd finetuning/baize/
python3 ./preprocess.py
```


目前定义的子任务如下表：

| 任务名 | 描述 | 示例 |
| ---- | ---- | ---- |
| `predict_painting` | 对指定的图像文件进行绘画推理 | *"对绘画进行心理学推理分析"* |
| `query_report` | 查询所有报告或者指定报告 | *"显示我的测试"* |
| `select_report` | 选中报告，将报告关联到上下文 | *"选择第1份评测"* |
| `start_questionnaire`| 启动 Questionnaire 流程 | *"用SCL进行评测"* |
| `stop_questionnaire`| 结束 Questionnaire 流程，用于强行终止 | *"结束吧"* |
| `questionnaire` | 执行进行中的 Questionnaire 流程 | *"我选C"* |
| `start_guide_flow` | 启动 Guide Flow 流程 | *"诊断一下我的心理健康状态"* |
| `stop_guide_flow` | 结束 Guide Flow 流程，用于强行终止 | *"结束"* |
| `guide_flow` | 执行进行中的 Guide Flow 流程 | *"我感觉还好"* |
| `start_appointment` | 启动预约流程 | *"我想预约心理咨询师"* |
| `stop_appointment` | 结束预约流程 | *"不约了"* |
| `appointment` | 执行进行中的预约流程 | *“周四上午可以”* |
| `super_admin` | 进入超级管理员模式，可以执行超级管理员指令 | *"大梦"* |


> 注意：预约流程是可以使用 Guide Flow 实现。

## 关联数据（Relation）

Relation 使用数据的 SN 进行数据精确查询：

- 报告。报告包括绘画报告和量表报告。
- 模板文章。模板文章是根据基础报告数据进行指定的数据内容描述。
- 融合数据。多数据源融合评测生成的报告数据。


## 记忆


## Guide Flow
