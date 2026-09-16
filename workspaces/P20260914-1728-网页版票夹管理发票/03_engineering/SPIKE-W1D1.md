# W1-D1 Spike 报告 —— Boot 4.1.1 × JDK 25 × MyBatis-Flex 1.11.8 组合验证

> 执行日期：2026-09-16（M1 W1 首日，行动项 A6）｜ 执行人：M1 开发会话（主 Agent）
> 结论：**五项冒烟 + 两个方言用例全部通过，无需 JDK 21 回退，主线方案放行** ✅
> 验证载体：`scaffold/server/`（spike 代码，W1 结束后随业务代码替换；`domain/spike/` 包整体移除）

## 1. 冒烟结果总表

| # | 冒烟项 | 结果 | 证据 |
| --- | --- | --- | --- |
| ① | 起服务 | ✅ | Spring Boot 4.1.1 + JDK 25（Zulu 25.0.4.1）启动 **2.4 秒**，三次重启均稳定 |
| ② | 连 PG | ✅ | HikariCP 连接池正常；`/api/v1/health` 返回 `{"status":"ok","db":"up"}`（含 SELECT 1 ping） |
| ③ | 事务 | ✅ | `@Transactional` 方法插入后抛异常 → 回滚成功（before=3, after=3, rolledBack=true） |
| ④ | 分页 | ✅ | Flex `paginate()` 走 PG 方言 LIMIT/OFFSET：page=1/size=2 → totalRow=3；第 2 页余 1 条；id DESC 排序正确 |
| ⑤ | session 落库 | ✅ | Spring Session JDBC：`spring_session` 表出现与响应 sessionId 完全一致的行，attributes 表 count=1；Cookie 名 `tw_session` 生效 |

## 2. PG 方言两个专项用例（tech-stack §3.3 待核实项②③）

| 用例 | 结果 | 结论 |
| --- | --- | --- |
| ILIKE 模糊检索 | ✅ | 库内含 `Spike Invoice A`/`INVOICE Service`：**Flex `QueryWrapper.like` 命中 0 条**（SQL LIKE 大小写敏感），**`@Select` 原生 ILIKE 命中 2 条**。→ 业务抬头检索**采用 @Select ILIKE**（tech-stack 预案的降级路径即为正解，非降级而是默认）；pg_trgm GIN 索引已随 V1 迁移建好 |
| enum TypeHandler | ✅（需 URL 参数） | Java enum ↔ PG enum 默认**不兼容**（`column "kind" is of type spike_kind but expression is of type character varying`）。→ **JDBC URL 加 `stringtype=unspecified`** 后读写全通，保持 PG enum 类型（无需降级 varchar+CHECK） |

## 3. 过程中的新发现（回填文档）

| # | 发现 | 处置 |
| --- | --- | --- |
| F1 | Maven Central 上存在 **`mybatis-flex-spring-boot4-starter:1.11.8`**（Boot 4 官方适配）与 **springdoc 3.1.1**（面向 Boot 4） | pom 采用两者；tech-stack 待核实项①解决 |
| F2 | Boot 4 模块化后，裸 `flyway-core` / `spring-session-jdbc` **不触发自动装配**，必须用 **`spring-boot-starter-flyway` / `spring-boot-starter-session-jdbc`** | pom 已改 starter 形态；W1 骨架沿用 |
| F3 | PG enum 列要求 **`stringtype=unspecified`**（见用例表） | 已写入 application.yml，注释说明 |
| F4 | Boot 4 删除了 `spring.session.store-type` 与 `spring.session.cookie-name`：仓库类型由 classpath 决定；Cookie 名改走 **`server.servlet.session.cookie.name`** | 已按新属性配置，Cookie 名实测为 `tw_session` |
| F5 | pgjdbc 不支持 TIMESTAMPTZ → `LocalDateTime` 读取（`Cannot convert the column of type TIMESTAMPTZ`）；插入反而可用，属隐蔽坑 | **实体时间字段一律 `OffsetDateTime`**（TIMESTAMPTZ 语义正配）；W1 业务表沿用 |
| F6 | Flex insert 会把 null 字段显式写入（不走 DB DEFAULT），`created_at NOT NULL` 被插 null 报错 | 审计字段在 service 层显式赋值（或 W1 骨架用 Flex `InsertListener` 统一处理，二选一，骨架阶段定） |
| F7 | BigDecimal 默认序列化为 JSON 数字（`4000.00`），与 api-design「金额字符串两位小数」不符 | W1 骨架的 Jackson 全局配置解决（ObjectMapper 定制），spike 未处理（非阻塞） |
| F8 | 本机 docker hub 拉取 `postgres:16-alpine` 极慢 | **仅 dev 环境**改用本地已有 `pgvector/pgvector:pg16`（标准 PG16 超集，contrib/pg_trgm 可用）；生产镜像仍按 tech-stack §6.1 用 postgres:16-alpine |

## 4. 资源预算初测（非正式，M3 W6 正式核验）

| 指标 | 预算 | 实测（spike） |
| --- | --- | --- |
| 启动耗时 | 非核心诉求 | **2.4s** |
| JVM RSS | ≤550MB | **39MB**（空载启动值，含 Web+Flex+Session+Flyway+springdoc 全家桶） |
| jar 体积 | 镜像 <350MB | **32MB**（叠加 temurin-25-jre-alpine 基座后余量充足） |

## 5. 环境与工具链记录

- JDK：Zulu 25.0.4.1（LTS）✅ ｜ Maven：系统 4.0.0-rc-5 构建 OK；**项目已生成 mvnw 锁 3.9.11**（tech-stack §3.2）
- PG：pgvector/pgvector:pg16 容器（`deploy/compose.yaml --profile dev`），库 ticketwallet/用户 tw
- Flyway V1 迁移（pg_trgm 扩展 + spike_kind enum + spike_items + GIN 索引）启动即执行成功

## 6. 对 W1 后续任务的影响

1. **契约链（W1④）**：springdoc 3.1.1 在 Boot 4 下 `/v3/api-docs` 输出 OpenAPI **3.1.0**（5 paths 已验证）——orval 对 3.1 的支持需在契约链搭建时验证，若不支则 springdoc 可降输出 3.0 语法（配置项）；
2. **auth 模块（W1⑤）**：session 机制已验证可用；登录锁定走 users 业务表（架构不变）；
3. **Jackson 金额序列化（F7）与统一异常/错误码包络**是 server 骨架（W1②）的首要件；
4. JDK 21 回退位：**未启用**（组合验证通过，回退预案保留不用）。

## 7. 复现命令

```bash
cd scaffold
docker compose -f deploy/compose.yaml --profile dev up -d postgres
cd server && ./mvnw package -DskipTests
java -jar target/server-0.1.0-SNAPSHOT.jar
# 冒烟：curl :8080/api/v1/health → POST/GET /api/v1/spike/items → /tx-rollback → /session
```
