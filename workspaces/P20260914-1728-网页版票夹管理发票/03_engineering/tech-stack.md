# 技术选型 —— 票夹通（TicketWallet）

> 文档版本：v1.0 ｜ 撰写日期：2026-09-14 ｜ 撰写角色：tech（技术负责人） ｜ 状态：待评审
> 上游依据：`00_charter/charter.md` G1–G5、`01_product/PRD.md` §5–§7、`01_product/features.md` F01–F15、`02_design/design-spec.md` §4/§6
> 下游读者：开发实施会话（M1–M3）、ops（部署）、support（故障排查口径）

---

## 1. 背景

票夹通是**单用户私有的网页版销项发票台账**：数据量小（单账号 ≤ 数千条）、写频低（月均 1–200 条）、读频高（筛选检索 ≤1s）、敏感度高（发票号码/金额/账号标识）。产品形态为响应式 Web App（桌面 ≥1024 / 移动 <768 双形态，见 IA §3），里程碑 M1（账号+录入+列表+检索）→ M2（汇总+导出+附件+回收站）→ M3（上线）。

技术选型的总约束：

1. **规模约束**：个人/小微场景，绝不能为「想象的规模」引入分布式、微服务、消息队列等复杂度；
2. **安全约束**（G4）：HTTPS 全站、密码不可逆存储、服务端会话可控失效、数据按账号强制隔离；
3. **性能约束**（G2）：1000 条内筛选 ≤1s、核心查询 P95 <500ms——单机关系型数据库加正确索引即可满足，无需缓存层；
4. **交付约束**（G5/M0）：脚手架必须能被后续开发会话直接物化，选型需「约定大于配置」、文档与社区资料充足；
5. **多会话协作约束**：本项目由多个 AI 开发会话接力实施，**强类型 + 显式契约（共享 Schema + OpenAPI）** 能显著降低会话间接管成本，这是比「最小依赖」更重要的考量。

## 2. 选型原则

| # | 原则 | 说明 |
| --- | --- | --- |
| T1 | 单体优先 | 一个前端应用 + 一个后端应用 + 一个数据库，Docker Compose 单机部署；拒绝一切 premature scaling |
| T2 | 类型贯穿 | 前端 TS、后端 TS、共享 zod Schema 生成请求校验与 API 类型，前后端契约单一来源 |
| T3 | 少依赖、长支持 | 只引入维护活跃、无重大 CVE 历史的依赖；组件自建为主（设计规范 §5 组件清单仅 7 个） |
| T4 | 可降级 | 每个关键组件给出降级路径（见 §8 与 engineering-plan §6），如 PG→SQLite、本地盘→MinIO |
| T5 | 现金流友好 | 全链路零许可费（MIT/Apache-2.0 开源 + 免费层级基建），M3 上线月成本目标 ≤ ¥50 |

## 3. 前端选型

### 3.1 框架：React 18 + TypeScript 5 + Vite 5

- **决策**：React 18（函数组件 + Hooks）+ TypeScript strict 模式 + Vite 构建。
- **理由**：
  1. 生态最成熟，AI 开发会话对 React + TS 的生成质量与纠错资料最充分（T4 多会话协作）；
  2. Vite 冷启动 <1s、HPR 极快，支撑「录入表单实时校验、价税合计实时重算」（R3）的迭代效率；
  3. 设计规范 §5 组件清单（Button/Input/Tag/Toast/Modal/Skeleton/Empty）全部可用受控组件精确实现，无需重型组件库。
- **备选与取舍**：
  - Vue 3 + Vite：模板语法更紧凑，但 TS 下 TanStack Query 等 hooks 生态在 React 侧更统一，放弃；
  - Next.js（SSR）：本项目是登录后私有台账，无 SEO/首屏公网内容诉求，SSR 只增加服务器渲染面（攻击面与运维成本），放弃；CSR + 静态托管 + 反代 /api 即可。
- **路由**：react-router-dom v7（声明式路由）。IA §2.2 的 9 条路由表直接映射；`/login?redirect=` 回跳、路由守卫（未登录 302）用 loader/context 实现。
- **数据层**：TanStack Query v5。列表/详情/汇总均为服务端状态，Query 的缓存失效（写操作后 `invalidateQueries` 自动重拉）天然满足 R4「写操作成功后以当前条件重新拉取列表」。
- **表单**：react-hook-form + zod resolver。校验规则（F04 字段表：号码 8–20 位、金额 >0 两位小数、日期 ≤ 今天等）以 zod schema 定义于共享包，blur 即时校验 + 提交全量校验（interactions §2.2 规则 2）。
- **样式**：Tailwind CSS v4 + CSS Variables。design-spec §4 全部 Tokens（色/字/距/圆角/阴影/动效/断点/z-index）落入 `src/styles/tokens.css` 的 CSS 变量，Tailwind theme 引用之——兑现 design-spec §8.2「Tokens 落入全局样式」的后继行动；`tabular-nums`、`prefers-reduced-motion` 以全局规则与工具类覆盖。
- **金额计算**：decimal.js。前端「价税合计 = 金额 + 税额」实时重算避免浮点误差（0.1+0.2 问题）；展示层统一两位小数 + 千分位（design-spec §4.2 规则）。
- **图表**：M2 汇总页「四象限类型分布」用**自建 CSS 横向条形图**（div 宽度百分比），不引入图表库——DP5 轻量克制，且分布图无交互诉求。
- **埋点**：自建 `lib/analytics.ts`，封装 features/interactions 定义的事件字典（30+ 事件），`navigator.sendBeacon` 批量上报至后端 `/api/v1/events`，避免引入第三方 SDK 的隐私与体积负担。

### 3.2 前端选型汇总表

| 领域 | 决策 | 备选 | 取舍理由 |
| --- | --- | --- | --- |
| 框架 | React 18 + TS 5 | Vue 3 | 生态/会话协作质量（T4） |
| 构建 | Vite 5 | webpack/Rspack | 开发体验与配置量 |
| 路由 | react-router-dom v7 | TanStack Router | 声明式够用、团队熟悉 |
| 服务端状态 | TanStack Query v5 | Redux Toolkit/RTKQ | 缓存失效模型贴合 R4 |
| 本地状态 | React state + Context | Zustand | 全局态仅「脱敏偏好」等少量 |
| 表单/校验 | react-hook-form + zod | Formik + yup | 与后端共享 zod schema（T2） |
| 样式 | Tailwind v4 + CSS Variables | styled-components | Tokens 直落（design-spec §8.2）、无运行时开销 |
| 数值 | decimal.js | big.js | 展示计算精度 |
| 图表 | 自建 CSS 条形 | recharts | 四象限分布无交互，DP5 |

## 4. 后端选型

### 4.1 框架：NestJS 10 + TypeScript 5

- **决策**：NestJS（Express adapter）+ Prisma ORM。
- **理由**：
  1. **模块化与 IA 对齐**：auth / invoices / attachments / stats / recycle / events 六个业务域恰好映射为六个 Nest module，目录即架构（见 architecture §4），多会话接力时可按模块认领；
  2. **Guard/Pipe 机制**：鉴权守卫（SessionAuthGuard）+ 所有权守卫（OwnershipGuard）+ Zod 校验管道，是「任意接口不可越权」（PRD §7）的骨架级保障，而非散落的 if 判断；
  3. **@nestjs/schedule**：cron 定时任务（回收站 30 天清理、会话清扫）开箱即用；
  4. OpenAPI（@nestjs/swagger）自动生成接口文档，与 api-design.md 互查。
- **备选与取舍**：
  - Fastify（裸）：更轻更快，但路由/校验/DI/定时全要手工搭，模块边界靠自觉——对小团队是劣势，放弃（仅在性能不达标时作为逃生舱）；
  - Go（Gin）/ Python（FastAPI）：运行时性能与内存更优，但与前端无法共享校验 Schema 与类型（T2 断裂），且单机单用户场景下 Node 的性能劣势不可感知（P95 <500ms 余量巨大），放弃。
- **校验**：zod（schema 定义在 `packages/shared`，NestJS 用 ZodValidationPipe 接管 DTO 校验）——同一份 schema 同时驱动前端表单、后端管道、导出的 API 类型，兑现 T2。
- **密码哈希**：**argon2id**（node-argon2，memoryCost 19MiB / timeCost 2 / parallelism 1，OWASP 2024 推荐参数）。备选 bcrypt（cost 12）：兼容性最好，若 argon2 原生模块在目标容器构建受阻则降级 bcrypt，哈希串带算法前缀（`$argon2id$` / `$2b$`）以支持共存识别。
- **会话**：**服务端 Session**（自建 `sessions` 表 + `HttpOnly; Secure; SameSite=Lax` Cookie，会话 ID 用 256bit 随机数）。不选 JWT：PRD 要求「退出后会话立即失效」「7 天过期」，JWT 服务端撤销需引入黑名单表，反而比 session 表更复杂；session 表天然支持「登出即删、过期即扫」。
- **CSRF**：SameSite=Lax 基线 + 写接口要求自定义头 `X-Requested-With: XMLHttpRequest`（双保险，纯表单跨站提交无法携带）。
- **CSV 生成**：csv-stringify（流式）。UTF-8 带 BOM（`\uFEFF` 前缀）满足 F10「Excel 直接打开不乱码」；≤5000 条流式写出内存峰值可控。
- **文件上传**：multer（内存接收 → 校验魔数 → 流写存储抽象层）。**格式校验同时校验扩展名与文件头魔数**（jpg/png/webp/pdf），防止改后缀绕过（F11 R7 `.zip` 拦截）。
- **日志**：pino（JSON 结构化日志）+ 请求 ID 中间件，供 support 按 requestId 检索故障。

### 4.2 后端选型汇总表

| 领域 | 决策 | 备选 | 取舍理由 |
| --- | --- | --- | --- |
| 框架 | NestJS 10 | Fastify 裸写 / Go / FastAPI | 模块化、Guard 体系、多会话协作（T4） |
| ORM | Prisma 5 | TypeORM / Drizzle | 迁移工作流成熟、类型生成质量高 |
| 校验 | zod + ZodValidationPipe | class-validator | 前后端共享 schema（T2） |
| 密码 | argon2id | bcrypt | OWASP 首选；bcrypt 为降级路径 |
| 会话 | 服务端 session 表 + Cookie | JWT | 退出即失效语义天然支持 |
| CSV | csv-stringify（流式 + BOM） | 手写拼串 | 转义边界（逗号/引号/换行）成熟 |
| 上传 | multer + 魔数校验 | @fastify/multipart | 与 NestJS FileInterceptor 集成顺滑 |
| 定时 | @nestjs/schedule | 系统 crontab | 随应用部署、可单测 |
| 日志 | pino | winston | 结构化、性能、生态 |

## 5. 数据库选型：PostgreSQL 16

- **决策**：PostgreSQL 16（Docker 官方镜像），Prisma 管理迁移。
- **理由**：
  1. **金额精度**：`numeric(12,2)` 原生定点类型，杜绝浮点累计误差——汇总口径（A3：正常+红冲负冲减）是产品核心承诺，不容 0.01 漂移；
  2. **检索能力**：抬头 contains 不区分大小写检索（F08）用 `ILIKE '%kw%'` + **pg_trgm GIN 索引**，1000 条内毫秒级，无需 ES；
  3. **软删除/条件过滤**：`deleted_at IS NULL` 部分索引（partial index）精准覆盖「回收站不参与列表/筛选/汇总」的口径隔离；
  4. 免费开源、Compose 单容器、备份即 `pg_dump` 一个 cron。
- **备选与取舍**：
  - SQLite：零运维最强，且为本项目**降级方案**（见 §8）——不作为主选的原因：并发写锁粒度粗（回收站清理 cron 与用户写入可能互斥）、pg_trgm/部分索引能力弱于 PG、生产镜像内嵌数据卷生命周期管理易出错；
  - MySQL 8：能力足够，但 numeric 语义与生成列习惯与 PG 无差异收益，社区镜像更新节奏略慢，放弃。
- **无缓存层（Redis）**：读多写少 + 单账号隔离，PG 直查 + 正确索引即满足 P95 <500ms；引入 Redis 只增加一个失效一致性维度，违反 T1。

## 6. 基础设施与部署

| 组件 | 决策 | 理由与备选 |
| --- | --- | --- |
| 运行方式 | Docker Compose（4 容器：caddy / api / postgres / web 静态产物由 caddy 托管） | 单机单命令部署（T1）；备选 K8s——明确不做，规模不配 |
| 反向代理/TLS | Caddy 2 | 自动 HTTPS（ACME）+ 静态托管 + 反代 `/api`，配置 10 行；备选 Nginx + certbot（手工续期脚本，运维成本高） |
| 附件存储 | 本地卷 `/data/attachments`，`StorageService` 抽象（put/get/delete/stat） | M2 单机最优；演进路径：实现 S3Provider 切 MinIO/OSS，业务代码零改动（T4） |
| 域名/TLS 证书 | Let's Encrypt（Caddy 自动） | 免费（T5），G4 HTTPS 达成 |
| 备份 | cron 每日 `pg_dump` + 附件目录 rsync，保留 14 份，异机（对象存储）冷备 | 容灾底线，详见 engineering-plan §6 |
| 监控 | `/api/v1/health` 健康检查 + 外部拨测（UptimeRobot 免费档）+ pino 日志落盘 | M3 轻量上线；APM（Sentry 免费档）为可选增强 |

## 7. 测试工具

| 层 | 工具 | 覆盖目标 |
| --- | --- | --- |
| 单测 | Vitest | zod schema、汇总口径计算（A3）、脱敏工具（design-spec §6.2）、CSV 转义、金额格式化 |
| API 集成 | Supertest + 测试库（每用例隔离） | F01–F12 全部 Given/When/Then、越权用例（F07 验收第 4 条） |
| E2E | Playwright（chromium + webkit） | interactions R1–R10 逐条抽用例（含异常分支表）、移动视口 375px 与桌面 1280px 双跑 |
| 类型 | tsc --noEmit + eslint | 契约漂移在 CI 拦截 |

## 8. 选型风险与降级路径（汇总）

| 选型 | 风险 | 降级/回退方案 |
| --- | --- | --- |
| argon2 原生模块 | 目标容器编译失败 | 哈希串算法前缀共存，切 bcrypt（§4.1） |
| PostgreSQL | 宿主资源 <1GB 或无 DB 运维能力 | Prisma 切 SQLite provider，业务代码不变（迁移重放） |
| 本地附件卷 | 磁盘故障/迁移上云 | StorageService 抽象切换 S3Provider（MinIO/OSS） |
| TanStack Query | 会话不熟悉 | 核心仅用 useQuery/useMutation/invalidate 三个 API，文档齐 |
| Caddy | 特殊网络环境不支持 ACME | 换 Nginx + certbot，仅动 deploy/ 目录 |
| NestJS | P95 不达标（概率极低） | 热点接口下沉 Fastify 路由共存改造；先加索引/查询优化 |

## 9. 验收标准与后续行动

### 9.1 验收标准

- [x] 前端/后端/数据库/基础设施/测试五域均有决策、理由、备选与取舍
- [x] 每个 G1–G5 目标均可追溯到选型支撑（G2→索引设计、G3→numeric+CSV 流式、G4→argon2/session/HTTPS、G5→本套文档与脚手架）
- [x] 关键组件均给出降级路径（T4），与 engineering-plan §6 兜底方案互链
- [x] design-spec §4 Tokens、§6.2 脱敏矩阵的技术落点已指明（Tailwind/CSS Variables、掩码工具函数）

### 9.2 后续行动

1. repo-layout.md 按本选型落目录（monorepo：apps/web + apps/server + packages/shared）。
2. architecture.md 细化模块边界、数据模型与关键数据流；api-design.md 定义接口契约与错误码。
3. M1 开工会话以本文档 §3.2/§4.2 两张汇总表作为依赖安装清单（锁定主版本）。
