---
name: _template
display_name: 技能模板
description: 这是一个技能模板，复制目录并改名后即可使用
keywords: 模板, template
when_to_use: 仅用于说明 SKILL.md 的写法，不要作为真实技能启用
version: 1.0
enabled: false
scope: global
---

# 技能标题

用一两句话说明这个技能解决什么问题、适用于什么场景。

## 何时使用

- 触发场景一
- 触发场景二

## 执行步骤

1. 第一步……
2. 第二步……
3. 第三步……

## 约束

- 必须遵守的边界与禁忌。
- 输出格式要求。

---

## 说明（此段不属于技能正文的一部分，使用时请删除）

- 目录约定：`assets/skills/<技能名>/SKILL.md`，技能名即 front-matter 的 `name`（缺失时取目录名）。
- 以 `_` 或 `.` 开头的目录/文件不会被加载，因此本模板目录不会生效。
- 技能的唯一权威来源是存储器（DB 表 `aigc_skill`），多实例共享；本目录是种子来源，
  通过 `aigc.properties` 的 `skills.seed=true` 在服务启动时幂等导入。
- `enabled: false` 的技能不会被注入；`scope` 留空或写 `global` 表示对所有 domain 生效，
  否则按 domain 隔离。
- 技能内容受 `skills.budget.ratio` 约束（占上下文窗口的百分比），超限会被截断并留痕。
