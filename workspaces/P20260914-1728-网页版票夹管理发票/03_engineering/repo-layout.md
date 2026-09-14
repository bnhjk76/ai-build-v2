# 工程目录规划 —— 票夹通（TicketWallet）

> 文档版本：v1.0 ｜ 撰写日期：2026-09-14 ｜ 撰写角色：tech（技术负责人） ｜ 状态：待评审
> 上游依据：`03_engineering/tech-stack.md`（选型）、`03_engineering/architecture.md` §3–§5（模块与数据模型）
> **说明：本目录树将被主 Agent 物化为 `03_engineering/scaffold/` 下的真实目录（上限 30 个目录）。文件不在物化范围，仅随开发会话在各目录内创建（文件级规划见 §3）。**

---

## 1. 背景

票夹通采用 **pnpm monorepo**：`apps/web`（React SPA）+ `apps/server`（NestJS 单体）+ `packages/shared`（三端共享契约）+ `deploy`（单机部署编排）。目录规划遵循三条规则：

1. **目录即架构**：后端六个业务模块（architecture §4.1）一一对应 `modules/` 下子目录，前端 `features/` 与之镜像，任何会话可按目录认领模块；
2. **收敛不动点**：跨模块复用物（7 个通用组件、脱敏/金额/埋点工具、守卫与管道、Prisma 客户端）各有唯一归属目录，禁止在业务目录内私建副本；
3. **测试同构**：每个 app 的测试紧邻源码（`tests/`、`test/`），共享包单测放包内。

## 2. 目录树（唯一物化依据）

下述树共 **30 个目录**（含叶子目录），均在物化上限内；目录行以 `/` 结尾，路径相对 `scaffold/` 根：

```
apps/
apps/web/
apps/web/public/
apps/web/src/
apps/web/src/api/
apps/web/src/components/
apps/web/src/features/
apps/web/src/lib/
apps/web/src/pages/
apps/web/src/styles/
apps/web/tests/
apps/web/tests/e2e/
apps/server/
apps/server/src/
apps/server/src/common/
apps/server/src/modules/
apps/server/src/modules/auth/
apps/server/src/modules/invoices/
apps/server/src/modules/attachments/
apps/server/src/modules/stats/
apps/server/src/modules/recycle/
apps/server/src/modules/events/
apps/server/src/infra/
apps/server/test/
packages/
packages/shared/
packages/shared/src/
deploy/
deploy/docker/
deploy/caddy/
```

## 3. 目录职责说明

| 目录 | 职责 | 关键内容（随开发创建） |
| --- | --- | --- |
| `apps/` | 应用工作区根 | — |
| `apps/web/` | 前端 React SPA 包 | `package.json`、`vite.config.ts`、`tsconfig.json`、`index.html` |
| `apps/web/public/` | 静态资源（不经构建） | favicon、字体回退说明文件 |
| `apps/web/src/` | 前端源码根 | `App.tsx`、`main.tsx`（路由表挂载点，IA §2.2 九路由在此声明） |
| `apps/web/src/api/` | HTTP 客户端与 API 类型 | `client.ts`（fetch 封装、401 拦截、X-Requested-With 头）、`invoices.ts`、`auth.ts` 等按资源的请求函数；类型由 shared 包派生 |
| `apps/web/src/components/` | 7 个通用组件（design-spec §5 清单即边界） | `Button` `Input` `Tag` `Toast` `Modal` `Skeleton` `Empty`，禁增私有组件 |
| `apps/web/src/features/` | 业务域目录（与后端模块镜像） | `auth/`（登录注册表单）、`invoice/`（录入表单、列表、筛选器、详情、附件区）、`stats/`（汇总卡+四象限条形图）、`recycle/`（回收站列表）；域内含各自 hooks 与 query 定义 |
| `apps/web/src/lib/` | 前端横切工具（architecture §3.1） | `mask.ts`（脱敏矩阵唯一实现）、`money.ts`（decimal 封装+千分位）、`analytics.ts`（埋点字典+sendBeacon）、`storage.ts`（pref_show_sensitive 等本地读写）、`draft.ts`（断网表单暂存） |
| `apps/web/src/pages/` | 页面组件（P01–P08，一文件一页） | `LoginPage` `RegisterPage` `InvoiceListPage` `InvoiceFormPage` `InvoiceDetailPage` `StatsPage` `RecyclePage` `NotFoundPage` |
| `apps/web/src/styles/` | 全局样式与 Design Tokens | `tokens.css`（design-spec §4 全量 Tokens → CSS 变量）、`base.css`（字体栈、tabular-nums、reduced-motion） |
| `apps/web/tests/` | 前端单测（Vitest） | 工具函数测试：`mask.test.ts`、`money.test.ts`、筛选参数序列化测试 |
| `apps/web/tests/e2e/` | Playwright E2E（R1–R10 用例骨架） | `r1-register.spec.ts` … `r10-stats.spec.ts`、`fixtures/`（登录态注入） |
| `apps/server/` | 后端 NestJS 包 | `package.json`、`nest-cli.json`、`tsconfig.json`、`.env.example` |
| `apps/server/src/` | 后端源码根 | `main.ts`、`app.module.ts`（六业务模块+infra 装配）、`global.d.ts` |
| `apps/server/src/common/` | 跨模块横切件 | `guards/`（SessionAuthGuard、OwnershipGuard）、`pipes/`（ZodValidationPipe）、`filters/`（统一异常过滤器→错误码）、`interceptors/`（requestId+日志）、`decorators/`（CurrentUser） |
| `apps/server/src/modules/` | 业务模块根 | — |
| `apps/server/src/modules/auth/` | F01–F03：注册/登录/登出/me；锁定计数 | controller、service、`session.service.ts` |
| `apps/server/src/modules/invoices/` | F04–F08/F10/F15：CRUD、五维筛选、CSV 导出、抬头联想；列表响应含**服务端掩码** | controller、service、`queries/`（筛选 SQL 组装）、`export.service.ts`（csv-stringify 流式+BOM） |
| `apps/server/src/modules/attachments/` | F11：上传/删除/流式下载；魔数校验、≤3 个 ≤10MB | controller、service（multer 接入） |
| `apps/server/src/modules/stats/` | F09：月/年汇总卡+四象限分布；**口径 A3 唯一实现点** | controller、`summary.service.ts` |
| `apps/server/src/modules/recycle/` | F06/F12：回收站列表/恢复/彻底删除/到期清理 | controller、service、`purge.service.ts`（cron 每小时） |
| `apps/server/src/modules/events/` | 埋点批量写入与 90 天清理 | controller、service |
| `apps/server/src/infra/` | 基础设施适配层 | `prisma.service.ts`、`schema.prisma`（ER 图落地）、`storage/`（StorageService 接口 + LocalProvider，S3Provider 预留）、`cron.service.ts`（回收站/会话/埋点清扫） |
| `apps/server/test/` | API 集成测试（Supertest + 测试库） | `auth.spec.ts`、`invoices.spec.ts`、`ownership.spec.ts`（**越权用例**，F07 验收第 4 条）、`stats.spec.ts`（口径 A3 断言）、`export.spec.ts`（BOM/列序） |
| `packages/` | 共享包工作区根 | — |
| `packages/shared/` | 三端共享契约包 | `package.json`（零运行时依赖） |
| `packages/shared/src/` | zod schema / 枚举 / 常量 | `invoice.schema.ts`（11 字段规则）、`filter.schema.ts`、`error-codes.ts`、`events.ts`（埋点字典）、`types.ts`（派生 TS 类型）、`index.ts` |
| `deploy/` | 单机部署编排 | `compose.yaml`（caddy/api/postgres 四服务）、`README` 部署步骤 |
| `deploy/docker/` | 服务镜像定义 | `api.Dockerfile`（多阶段构建）、`web.Dockerfile`（构建产物注入 caddy 镜像，仅 M3 用） |
| `deploy/caddy/` | 入口配置 | `Caddyfile`（自动 HTTPS、静态托管、/api 反代、body 上限 15MB）、`html/`（SPA 回退首页占位） |

### 3.1 根级文件（不在物化范围，开发会话创建）

`scaffold/` 根的 `package.json`（pnpm workspace）、`pnpm-workspace.yaml`、`.gitignore`、`.env.example`、`README.md`。

## 4. 目录 ↔ 模块 ↔ 里程碑对照

| 里程碑 | 涉及目录（新增/首版实现） |
| --- | --- |
| M1 | `packages/shared/src`、`common/`、`modules/auth`、`modules/invoices`（CRUD+筛选，导出桩）、`infra`（Prisma+schema）、`web` 的 api/components/features(auth,invoice)/lib/pages(login,register,list,form,detail,404)/styles、`apps/*/test*` 首批用例 |
| M2 | `modules/invoices` 导出补全、`modules/attachments`、`modules/stats`、`modules/recycle`、`modules/events`、web `features(stats,recycle)`、`pages(StatsPage,RecyclePage)`、`storage/` 本地实现、E2E 补 R5–R10 |
| M3 | `deploy/*` 全量启用（TLS 域名、备份 cron）、拨测与健康检查接入 |

## 5. 约束与纪律（写给后续开发会话）

1. **不新增顶层目录**：如确需（如 `apps/worker`），须经主 Agent 变更本文件并升版本号；
2. `modules/*` 之间**禁止横向 import**（recycle 对 invoices 的委托经 service 接口注入，attachments 联动经事件/service 方法）——保证模块可独立测试；
3. `web/src/components` 仅放 design-spec §5 清单内 7 件，业务组件一律进 `features/*`；
4. `packages/shared` **不得依赖任何 app**（依赖方向单向：apps → shared）；
5. 测试文件与被测对象同名（`mask.ts` ↔ `mask.test.ts`），E2E 文件名前缀 `r{编号}` 对应 interactions 流程编号，便于验收反查。

## 6. 验收标准与后续行动

### 6.1 验收标准

- [x] 目录树在单一代码围栏内、30 个目录（≤上限）、相对路径、目录行以 `/` 结尾
- [x] 每个目录有唯一职责说明，与 architecture.md §3/§4 模块图一一对应
- [x] 里程碑 × 目录对照表可指导 M1–M3 增量落地
- [x] 明确了物化范围（仅目录）与文件创建时机的边界

### 6.2 后续行动

1. 主 Agent 将 §2 目录树物化至 `03_engineering/scaffold/`；
2. M1 开工会话按 §4 对照表从 `packages/shared/src` 起步（schema 先行，前后端并行）；
3. 首次提交时按 §3.1 补根级配置文件并初始化 pnpm workspace。
