---
description: 检索全局知识库：按关键词列出相关文档条目
argument-hint: <关键词> [--project <项目ID>]
---

参数：$ARGUMENTS

检索知识库索引 `workspaces/_knowledge/index.md`：

1. 解析参数中的关键词（`--project` 用于限定项目 ID，默认全部）；
2. 对标题/标签/摘要/路径做关键词匹配，按命中数排序，取前 8 条；
3. 输出列表：标题、角色、项目 ID、原路径、一句话摘要；
4. 如无命中，提示可先运行 `/flow:run` 产出文档入库。
