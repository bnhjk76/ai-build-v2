# 第 8 章 部署上线与成本预算

> 学习线：🚀 工程 ｜ 对应真实文件：`WS/…/scaffold/deploy/`（Dockerfile.server、Caddyfile、compose.prod.yaml、backup.sh）

## 8.1 部署全景：三个容器各司其职

```
浏览器 ──HTTPS──▶ Caddy（门卫：管证书/静态文件/转发）
                    ├──▶ api 容器（Spring Boot，跑 Java 程序）
                    └──▶ postgres 容器（数据之家）
                 全部由 docker compose 一条命令编排
```

- **Docker**：把程序+环境打包成「镜像」，在任何机器上运行结果都一样——解决「我电脑上明明能跑」；
- **Caddy**：反向代理，自动申请续期 HTTPS 证书（浏览器地址栏的小锁），还负责屏蔽内部接口、限制上传体积；
- **预算约束**：本项目目标月成本 ≤¥50（2C2G 小服务器），所以每个组件都限了内存。

## 8.2 省钱是工程能力（真实实测数据）

| 项 | 预算 | 实测 | 怎么做到的 |
| --- | --- | --- | --- |
| api 镜像体积 | <350MB | **271MB** | 用 jre-alpine 基础镜像（只带运行时，不带编译器） |
| api 内存 RSS | ≤550MB | **290MB** | JVM 参数：堆限 60% + SerialGC（小内存专用回收器） |

一句 JVM 参数 `-XX:MaxRAMPercentage=60 -XX:+UseSerialGC` 省下一个月几十块的升配费——**预算意识从写代码第一天开始**。

## 8.3 备份：数据产品的事故险

`deploy/backup.sh`：每天凌晨 2 点导出整库（含用户、发票、会话表）+ 附件目录快照，滚动保留 14 份。
设计细节里的善意：同步**不带 `--delete`**——用户今天误删文件，昨天的备份不会被同步逻辑连带删掉（防误删传播）。

## 8.4 本地一键跑通整个项目（附录）

```bash
cd workspaces/P20260914-1728-网页版票夹管理发票/03_engineering/scaffold
docker compose -f deploy/compose.yaml --profile dev up -d postgres   # ① 数据库
cd server && ./mvnw spring-boot:run &                                # ② 后端 :8080
cd ../web && pnpm dev                                                # ③ 前端 :5173
# 浏览器打开 http://localhost:5173
```

## 8.5 上线前检查单（Checklist 文化）

- [ ] 契约三道防线全绿
- [ ] 集成 + E2E 测试全绿
- [ ] 镜像/内存预算实测达标
- [ ] 备份跑过一次**恢复演练**（备份没恢复过 = 没有备份）
- [ ] 敏感配置（数据库密码）不在代码库里（.env 已被 .gitignore）

## 练习（结业项目建议）

按本教程的完整路线独立做一个小项目（推荐「个人记账本」）：
章程（含范围外清单）→ 3 条以上 Given-When-Then → 2 天 Spike → 三层后端 + 前端一页 → 10 个集成测试 + 5 个 E2E → Dockerfile + 预算实测。
做完它，你就把这本教程的每一步都亲手走了一遍。
