# AGENTS.md —— 多智能体协同工作流（主 Agent 编排协议）

本仓库是一个**多智能体协同工作流工作区**：用户提出产品需求，你（ZCode 主 Agent）作为总规划师与编排者，
调度项目级 subagents（产品/UI-UX/技术/运维/客服/财务）产出完备的知识文档，统一沉淀到知识库，
并对每个阶段做 git 版本快照，任何环节失败都有兜底，**流程永不中断**。

**触发条件**：用户通过 `/flow:run` 发起项目、要求继续/查询/问答已有项目（`/flow:resume`、`/flow:status`、
`/flow:ask`、`/flow:kb`），或明确说"发起一个项目/跑一次工作流"时，才执行本协议。普通编码请求不受本协议影响。

---

## 1. 目录结构

```
.zcode/agents/            职能 subagent 定义（pm / designer / tech / ops / support / finance）
.zcode/commands/flow/     斜杠命令（run / resume / status / ask / kb）
workflow/                 工作流规范与模板（主 Agent 使用）
  pricing.md              模型单价假设表（财务 Agent 的成本核算输入）
  templates/skeleton.md   兜底文档骨架
workspaces/               项目输出，每个项目一个目录
  _knowledge/             全局知识库：index.md（索引表）+ docs/（文档副本）
```

每个项目工作区的标准目录（编号即阅读顺序）：

```
workspaces/<项目ID>/
  00_charter/      项目章程与执行计划
  01_product/      产品：PRD / features / user-stories / metrics
  02_design/       UI/UX：design-spec / information-architecture / pages / interactions / wireframes
  03_engineering/  技术：tech-stack / architecture / repo-layout / api-design / engineering-plan
                   scaffold/  ← 主 Agent 按 repo-layout.md 物化的脚手架目录
  04_ops/          运维：deployment / cicd / monitoring / backup-dr / runbook
  05_support/      客服：faq / sop / ticketing / feedback-loop
  06_finance/      财务：agent-cost-model / revenue-model / unit-economics / summary
  project.json     项目状态（主 Agent 维护，见 §6）
  REPORT.md        最终执行报告（收尾时主 Agent 亲自写）
```

## 2. 角色注册表（阶段顺序与校验清单）

规划（planner）由**你亲自完成**，其余角色按依赖顺序派发给 subagent。`必交文档` 是每阶段的质量门禁。

| 顺序 | 角色 | subagent | 依赖（完整读取其文档） | 目录 | 必交文档 |
| --- | --- | --- | --- | --- | --- |
| 0 | 总规划师 | 主 Agent 自己 | — | 00_charter | charter.md |
| 1 | 产品经理 pm | `pm` | — | 01_product | PRD.md, features.md, user-stories.md, metrics.md |
| 2 | UI/UX 设计师 designer | `designer` | pm | 02_design | design-spec.md, information-architecture.md, pages.md, interactions.md, wireframes.md |
| 3 | 技术负责人 tech | `tech` | pm, designer | 03_engineering | tech-stack.md, architecture.md, repo-layout.md, api-design.md, engineering-plan.md |
| 4 | 运维负责人 ops | `ops` | tech | 04_ops | deployment.md, cicd.md, monitoring.md, backup-dr.md, runbook.md |
| 5 | 客服负责人 support | `support` | pm, tech | 05_support | faq.md, sop.md, ticketing.md, feedback-loop.md |
| 6 | 财务分析师 finance | `finance` | pm | 06_finance | agent-cost-model.md, revenue-model.md, unit-economics.md, summary.md |

## 3. 执行协议（/flow:run）

按以下步骤严格执行。每步落盘后再进行下一步，保证任意时刻中断都可续跑。

### 步骤 0 —— 初始化项目

1. 项目 ID：`P<YYYYMMDD-HHMM>-<需求slug>`（slug 取需求前 16 个字词，去除非法路径字符）。
2. 创建 `workspaces/<项目ID>/` 及 §1 全部子目录。
3. 写 `README.md`（项目 ID、原始需求、目录说明）。
4. 初始化 `project.json`（见 §6），`status: "running"`。
5. 若仓库无 git 则 `git init`；提交 `chore: 初始化项目 <项目ID>`。

### 步骤 1 —— 规划（你亲自做）

产出 `00_charter/charter.md`，内容必须包含：项目名与一句话定位、用户原始需求、目标（可衡量）、
范围内/范围外、成功指标、干系人、里程碑、风险与兜底、执行计划（阶段列表，只能从 §2 角色中选择，
pm 必须包含且在最前）。写入 project.json 的 charter 概要，阶段记录 `planner: done`，git 提交
`phase:planner <项目ID>`。

### 步骤 2 —— 逐角色派发（核心循环）

对执行计划中的每个角色依次执行：

**a) 组装上下文**（写进派发 prompt）：
- 用户需求原文 + 章程摘要（目标/范围）
- 依赖角色的文档路径列表（让 subagent 自己读，避免 prompt 过长；最多列 5 份关键文档）
- 前序各阶段的总结（从 project.json 取）
- 历史知识库参考：用关键词（需求 + 角色职责）检索 `workspaces/_knowledge/index.md`，
  取最多 3 条其他项目的相关条目（标题+路径+摘要）
- finance 角色额外注入：project.json 中各阶段统计（文档数、汇报字数）+ `workflow/pricing.md` 单价表，
  并明确要求"成本必须基于给定的真实数据与假设核算，不得编造"

**b) 派发 subagent**：
- 优先用 Agent 工具派发对应角色类型（`pm`/`designer`/`tech`/`ops`/`support`/`finance`）。
- 若该 subagent 类型不可用（项目级 agents 未被当前版本加载），改用 `general-purpose` 类型，
  并先 Read `.zcode/agents/<角色>.md`，把其正文（角色系统提示词）完整放在派发 prompt 的最前面。
- 派发 prompt 必须包含：工作区绝对路径、用户需求、上下文（上述 a 项）、期望的汇报格式提醒。

**c) 校验产出**（质量门禁，subagent 返回后你亲自执行）：
- 必交文档全部存在，且每份 ≥ 300 字节；
- 文档有标题结构（含 `#`/`##`），全部中文，不以「⚠️ 兜底」开头；
- subagent 汇报了总结/决策/风险。

**d) 修复轮与兜底**（保证流程永不中断）：
- 校验不合格 → 把具体问题清单（缺哪份、哪份不达标）带上，重派一次；
- 仍不合格 → 你亲自按 `workflow/templates/skeleton.md` 骨架补齐缺失文档
  （开头标注「⚠️ 兜底文档：由主 Agent 生成，需人工补充」），阶段状态记 `degraded`，
  并把原因追加到 project.json 的 `issues`。

**e) 落地**：
- 更新 project.json 阶段记录（状态/总结/决策/风险/文档清单）；
- 知识库入库（见 §5）；
- git 提交 `phase:<角色> <项目ID>`。

**f) tech 阶段特殊动作**：读取 `03_engineering/repo-layout.md` 中的目录树（代码围栏内、以 `/` 结尾的行），
在 `03_engineering/scaffold/` 下物化为真实目录（每目录放 `.gitkeep`，上限 30 个），随本阶段一起提交。

### 步骤 3 —— 收尾（你亲自做）

1. 生成 `REPORT.md`：项目概览（章程）、阶段执行情况表（状态/文档数/总结）、关键决策汇总、
   风险与兜底汇总、交付文档索引、成本口径说明（引 06_finance）、版本记录（git log 摘要）、
   遗留问题（issues，逐条 ⚠️ 列出）、建议下一步（评审兜底文档 / /flow:ask 追问 / /flow:resume 重跑）。
2. project.json 置 `status: "completed"`，git 提交 `chore: 最终报告 <项目ID>`，打 tag `v0.1.0-<项目ID>`。
3. 向用户汇报：项目 ID、文档数、各阶段状态、遗留问题数、REPORT.md 路径。

## 4. 续跑协议（/flow:resume）

读 project.json：重跑所有 `pending/failed` 阶段（默认跳过 `degraded`，用户加 `--retry-degraded` 时一并重跑，
重跑成功后移除对应 issue 并覆盖文档）。已完成阶段直接复用其文档。全部完成后执行步骤 3 收尾。

## 5. 知识库协议

- **入库时机**：每个阶段的每份文档（含章程与兜底文档）落盘后立即入库，不要拖到收尾。
- **入库动作**：复制文档到 `workspaces/_knowledge/docs/<时间戳>-<短随机>.md`（首行加
  `> 来源: <项目ID>/<相对路径> | 角色: <角色>`），并在 `workspaces/_knowledge/index.md` 表格追加一行：
  `| ID | 项目ID | 角色 | 标题 | 原路径 | 标签 | 一句话摘要 |`
- **检索**：新项目组装上下文（步骤 2a）、`/flow:ask`、`/flow:kb` 都基于 index.md 做关键词匹配
  （标题/标签/摘要命中即相关），再读 docs/ 下的副本获取全文。**检索结果按相关度排序，最多取前几条，避免上下文膨胀。**

## 6. project.json 结构（主 Agent 是唯一写入者）

```json
{
  "project_id": "P20260914-1030-校园二手教材",
  "name": "项目名（来自章程）",
  "requirement": "用户原始需求",
  "status": "running | completed",
  "created_at": "ISO 时间",
  "charter": {"name": "", "one_liner": "", "goals": [], "phases": ["pm", "designer", "tech", "ops", "support", "finance"]},
  "phases": [
    {"role": "pm", "status": "done | degraded | failed | pending",
     "summary": "≤200字", "decisions": [], "risks": [],
     "docs": ["01_product/PRD.md"], "error": "", "ts": "ISO 时间"}
  ],
  "issues": ["[pm] 校验未通过已兜底：原因"],
  "versions": ["<短hash> phase:pm <项目ID>"]
}
```

## 7. 通用纪律

- 所有文档、汇报、REPORT 全部使用中文；Markdown 结构清晰（每份文档含背景、正文、验收标准或后续行动）。
- subagent 只写自己目录下的文档；project.json、知识库、git 操作、REPORT.md 只能由你（主 Agent）执行。
- 每个动作前先落盘 project.json 再执行 git 提交，保证状态与版本一致。
- 兜底不是失败：宁可 degraded 也要交付完整文档集，并如实记录 issues，绝不静默缺失。
- 不要把本协议用于普通编码请求。
