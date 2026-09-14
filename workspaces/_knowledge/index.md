# 全局知识库索引

> 所有项目所有角色的产出文档统一入库于此。主 Agent 在新项目组装上下文、`/flow:ask`、`/flow:kb`
> 时检索本表，文档全文在 `docs/` 下。

| ID | 项目 | 角色 | 标题 | 原路径 | 标签 | 摘要 |
| --- | --- | --- | --- | --- | --- | --- |
| 20260914172937-64d065 | P20260914-1728-网页版票夹管理发票 | planner | 项目章程 —— 票夹通（网页版发票票夹管理） | 00_charter/charter.md | 章程,规划,发票,票夹 | 票夹通项目章程：定位/目标G1-G5/范围/里程碑/风险兜底/六阶段执行计划 |
| 20260914173632-cf3a97 | P20260914-1728-网页版票夹管理发票 | pm | PRD —— 票夹通（TicketWallet）网页版发票票夹 | 01_product/PRD.md | PRD,产品,发票,票夹 | 票夹通PRD：开票方视角定位、个人/小微商户双画像、6痛点、G1-G5口径与范围边界 |
| 20260914173632-372fce | P20260914-1728-网页版票夹管理发票 | pm | 功能清单与验收标准 —— 票夹通（TicketWallet） | 01_product/features.md | 功能清单,MoSCoW,验收标准 | F01-F15功能清单：MoSCoW优先级+M1/M2里程碑映射+Given/When/Then验收标准含异常路径 |
| 20260914173632-44b1f1 | P20260914-1728-网页版票夹管理发票 | pm | 用户故事与关键用户旅程 —— 票夹通（TicketWallet） | 01_product/user-stories.md | 用户故事,用户旅程 | US01-US15用户故事与J1-J4关键旅程，覆盖八项能力的正常/异常路径与覆盖度矩阵 |
| 20260914173632-f0d401 | P20260914-1728-网页版票夹管理发票 | pm | 成功指标与埋点口径 —— 票夹通（TicketWallet） | 01_product/metrics.md | 指标,埋点,北极星 | 北极星指标（周活本月新增开票条数）+P1-P4过程指标+质量护栏+30余埋点事件字典 |
| 20260914174449-1fb490 | P20260914-1728-网页版票夹管理发票 | designer | 设计规范（Design Spec）—— 票夹通（TicketWallet） | 02_design/design-spec.md | 设计规范,DesignTokens,脱敏 | 设计原则+视觉语言+DesignTokens（hex/px实值）+组件规范+脱敏矩阵（Q2回签：抬头不脱敏） |
| 20260914174449-67d0a0 | P20260914-1728-网页版票夹管理发票 | designer | 信息架构 —— 票夹通（TicketWallet） | 02_design/information-architecture.md | 信息架构,导航,概念模型 | 站点地图+桌面顶栏/移动底部Tab双端导航+核心概念模型+术语表+权限矩阵 |
| 20260914174449-414d3e | P20260914-1728-网页版票夹管理发票 | designer | 页面清单 —— 票夹通（TicketWallet） | 02_design/pages.md | 页面清单,状态设计 | P01-P08页面清单：每页目的/关键元素/默认加载空错误无权限五状态 |
| 20260914174449-4d8abc | P20260914-1728-网页版票夹管理发票 | designer | 交互流程 —— 票夹通（TicketWallet） | 02_design/interactions.md | 交互流程,异常兜底 | R1-R10交互流程：正常流+异常兜底含mermaid图，与features验收条目逐一映射 |
| 20260914174449-d3487f | P20260914-1728-网页版票夹管理发票 | designer | 线框图 —— 票夹通（TicketWallet） | 02_design/wireframes.md | 线框图,布局 | W1-W8 ASCII线框图：列表页含桌面/移动双形态，可直接指导实现 |
| 20260914175358-a01893 | P20260914-1728-网页版票夹管理发票 | tech | 技术选型 —— 票夹通（TicketWallet） | 03_engineering/tech-stack.md | 技术选型,架构选型 | 五域选型：React18+Vite+TS/NestJS+Prisma+PG16/Caddy，各决策理由备选取舍+降级路径 |
| 20260914175358-ea9a2a | P20260914-1728-网页版票夹管理发票 | tech | 系统架构 —— 票夹通（TicketWallet） | 03_engineering/architecture.md | 架构,ER图,数据流 | 单机单体架构：部署拓扑、前后端模块划分、ER图与索引策略、六条关键数据流mermaid |
| 20260914175358-a70c62 | P20260914-1728-网页版票夹管理发票 | tech | 工程目录规划 —— 票夹通（TicketWallet） | 03_engineering/repo-layout.md | 目录规划,脚手架 | 30目录monorepo目录树（apps/web+apps/server+packages/shared+deploy）+职责表+里程碑对照 |
| 20260914175358-e63f44 | P20260914-1728-网页版票夹管理发票 | tech | API 设计 —— 票夹通（TicketWallet） | 03_engineering/api-design.md | API设计,错误码 | 22个接口+15个错误码+统一约定+安全约定（Session/越权404/服务端掩码） |
| 20260914175358-fedc38 | P20260914-1728-网页版票夹管理发票 | tech | 工程计划 —— 票夹通（TicketWallet） | 03_engineering/engineering-plan.md | 工程计划,排期,测试 | M1-M3周级排期DoD、trunk-based分支、四层测试、降级/回滚/容灾兜底 |
| 20260914175951-339ba5 | P20260914-1728-网页版票夹管理发票 | ops | 部署方案 —— 票夹通（TicketWallet） | 04_ops/deployment.md | 部署,上线,回滚 | 三环境划分（dev/staging/生产同VPS三重隔离）、12步上线流程、三层回滚，成本≤¥50/月 |
| 20260914175951-50ebf0 | P20260914-1728-网页版票夹管理发票 | ops | CI/CD 方案 —— 票夹通（TicketWallet） | 04_ops/cicd.md | CICD,流水线,发布策略 | PR/夜间/tag三触发器流水线、30秒健康门禁自动回退、周四21:00发布窗口滚动重建 |
| 20260914175951-b27da6 | P20260914-1728-网页版票夹管理发票 | ops | 监控告警方案 —— 票夹通（TicketWallet） | 04_ops/monitoring.md | 监控,告警,日志 | 四黄金信号阈值表（P95<500ms/可用性≥99.5%）、pino日志方案、P0-P1-P2告警分级与值班升级 |
| 20260914175951-a4c9b2 | P20260914-1728-网页版票夹管理发票 | ops | 备份与容灾方案 —— 票夹通（TicketWallet） | 04_ops/backup-dr.md | 备份,容灾,RTO,RPO | RPO24h/RTO2h矩阵、版本化桶防误删传播、pg_dump AES-256加密、季度演练为一票否决项 |
| 20260914175951-1c6e25 | P20260914-1728-网页版票夹管理发票 | ops | 故障处置手册（Runbook）—— 票夹通（TicketWallet） | 04_ops/runbook.md | runbook,故障处置 | 通用排查三步+10类故障卡片（现象/定位/处置/升级路径），手册化到可照抄执行 |
| 20260914180756-4a8250 | P20260914-1728-网页版票夹管理发票 | support | 客服 FAQ —— 票夹通（TicketWallet） | 05_support/faq.md | FAQ,话术,错误码 | 八大主题58条Q/A对+15错误码速查表（与api-design逐字对齐），话术可直接发送 |
| 20260914180756-2e431d | P20260914-1728-网页版票夹管理发票 | support | 客服 SOP（接待 / 投诉 / 退款补偿）—— 票夹通（TicketWallet） | 05_support/sop.md | SOP,投诉,补偿 | 接待五步/投诉五步法/8条话术红线/免费口径补偿工具箱/四个专项预案 |
| 20260914180756-4b32f0 | P20260914-1728-网页版票夹管理发票 | support | 工单体系（分级 / SLA / 升级 / 回访）—— 票夹通（TicketWallet） | 05_support/ticketing.md | 工单,SLA,升级路径 | P0-P3四级（与ops告警显式映射）、SLA到分钟（P0首响15min）、四层升级链含电话兜底 |
| 20260914180756-8d8572 | P20260914-1728-网页版票夹管理发票 | support | 反馈闭环 —— 票夹通（TicketWallet） | 05_support/feedback-loop.md | 反馈闭环,回流 | 四渠道收集（含埋点被动信号）、类型×影响分级、五条回流去向与节拍表 |
