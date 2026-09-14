---
description: 断点续跑一个项目工作流：重跑未完成/失败的阶段并收尾
argument-hint: <项目ID> [--retry-degraded]
---

参数：$ARGUMENTS

请按 AGENTS.md 的「续跑协议」执行：

1. 读取 `workspaces/<项目ID>/project.json`，展示当前各阶段状态；
2. 重跑所有未完成/失败的阶段（若参数含 `--retry-degraded`，连同 degraded 阶段一起重跑，
   重跑成功后从 issues 移除对应条目）；
3. 全部阶段完成后执行收尾（REPORT.md 更新、git 提交与 tag）。

若项目已完成且未指定 `--retry-degraded`，告知用户并给出建议。
