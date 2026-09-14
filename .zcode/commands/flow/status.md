---
description: 查看项目工作流状态：阶段进展、文档、遗留问题与版本记录
argument-hint: [项目ID]（省略则列出全部项目）
---

参数：$ARGUMENTS

- 若未给出项目 ID：列出 `workspaces/` 下所有含 project.json 的项目（ID、状态、名称）。
- 若给出项目 ID：读取其 project.json，展示：项目名与需求、各阶段状态表（角色/状态/文档数/总结）、
  遗留问题（issues）、git 版本记录（`git log --oneline -- workspaces/<项目ID>`）、文档清单。
