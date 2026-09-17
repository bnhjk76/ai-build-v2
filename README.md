# ai-build-v2 —— 多 Agent 协作工作流 + 全栈实战项目「票夹通」

> 一个仓库，三样东西：**多智能体协作工作流**、**一个从需求到上线全流程真实交付的全栈项目**、**一套面向入门者的实战教程**。

## Git 访问

**仓库地址**：https://github.com/bnhjk76/ai-build-v2

```bash
# SSH（推荐，需先在 GitHub 配置公钥）
git clone git@github.com:bnhjk76/ai-build-v2.git

# 或 HTTPS
git clone https://github.com/bnhjk76/ai-build-v2.git
```

- 分支：`main`（唯一长期分支，trunk-based）
- 标签：`v0.1.0-P20260914-1728-网页版票夹管理发票`（M0 文档里程碑快照）
- 推送免密：本机已配置 ed25519 SSH 密钥（`~/.ssh/id_ed25519`，公钥已登记 GitHub 账号）

## 这个仓库是什么

用户提出一句话需求（「我想做一个网页版票夹管理」），主 Agent 按 [AGENTS.md](AGENTS.md) 协议调度 6 个职能子 Agent（产品/设计/技术/运维/客服/财务），经过 8 个阶段产出 31 份文档与可运行代码，全程质量门禁 + git 快照 + 决策记录，**零兜底零中断**完成 M1/M2 双里程碑冻结。

## 仓库结构

```
AGENTS.md                    多 Agent 协作协议（主 Agent 编排规则）
workflow/                    工作流规范与模板
workspaces/                  项目输出
  └── P20260914-1728-网页版票夹管理发票/
      ├── 00_charter ~ 06_finance    8 阶段知识文档（31 份）
      ├── 07_compliance/             合规评估（个保法逐条对照）
      └── 03_engineering/scaffold/   可运行代码（Java 后端 + React 前端 + 契约 + 部署）
tutorial/                    入门教程《从零到上线》（9 章 + 25 坑实录）
```

## 技术栈

- **后端**：Spring Boot 4.1.1 · JDK 25 LTS · MyBatis-Flex · PostgreSQL 16 · Flyway · Spring Security(Argon2/Session) · Maven
- **前端**：React 19 · TypeScript(strict) · Vite · Tailwind v4 · TanStack Query · react-hook-form + zod
- **契约**：springdoc → OpenAPI 工件 → orval 生成客户端 → CI 三道防线防漂移
- **部署**：Docker Compose 三容器 · Caddy 自动 HTTPS · 每日备份
- **AI 协作（本项目的"隐形队员"）**：
  - 智能体框架：ZCode（主 Agent 编排 + 6 个职能 subagent 派发）
  - 模型版本：**GLM-5.3**（`bigmodel-individual-coding-plan`，智谱 BigModel）——贯穿规划、6 大职能角色、开发与测试全程
  - 人类角色：需求提出 · D1–D7 决策拍板 · 环境准备 · 最终验收

## 快速开始

```bash
cd workspaces/P20260914-1728-网页版票夹管理发票/03_engineering/scaffold
docker compose -f deploy/compose.yaml --profile dev up -d postgres   # ① 数据库
cd server && ./mvnw spring-boot:run &                                # ② 后端 :8080
cd ../web && pnpm install && pnpm dev                                # ③ 前端 :5173
```

测试：`./mvnw test`（19 项集成+性能）｜ `pnpm exec playwright test`（34 项 E2E 双视口）

## 质量现状

| 项 | 结果 |
| --- | --- |
| 集成 + 性能测试 | 19/19 绿 |
| E2E（R1–R10 全流程，375px/1280px 双视口） | 32 绿 + 2 skip（有因） |
| G2 性能（1000 条五维筛选） | P95 = 31ms（预算 500ms） |
| 生产预算 | 镜像 271MB / RSS 290MB（预算 350/550MB） |

## 学习路径

入门者请直接读 [tutorial/README.md](tutorial/README.md)——四条学习线（多 Agent 协作 / Java / Web / 工程实践），以本仓库真实代码为教材，每章有动手练习。
