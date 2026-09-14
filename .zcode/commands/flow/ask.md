---
description: 基于知识库回答问题（默认限定指定项目，加 --all 跨全部项目检索），回答须引用来源文档
argument-hint: <项目ID> <问题>（或 --all <问题>）
---

参数：$ARGUMENTS

请基于知识库回答用户问题：

1. 检索 `workspaces/_knowledge/index.md`（未指定项目或含 `--all` 时检索全部；否则限定该项目 ID 的条目），
   按关键词相关度取最多 4 条；
2. 用 Read 读取命中文档在 `workspaces/_knowledge/docs/` 下的副本（每份最多取前 2500 字）；
3. 只依据资料回答，中文作答；资料不足以回答时明确说明，不要编造；
4. 回答末尾列出引用来源（标题 + 原路径）。
