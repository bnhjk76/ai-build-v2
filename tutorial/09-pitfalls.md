# 附录 踩坑实录大全（20+ 个真实坑）

> 本项目开发全过程真实踩过并解决的坑，按主题分组。排查问题时当手册翻；
> 教学时当案例库用——**每个坑背后都是一条工程经验**。

## A. 新版本组合类（Boot 4 / JDK 25 生态）

| # | 坑 | 现象 | 解法 |
| --- | --- | --- | --- |
| 1 | Spring Boot 4 模块化后，裸依赖不触发自动装配 | Flyway 建表根本没执行（库是空的） | 换 starter 形态：`spring-boot-starter-flyway` / `spring-boot-starter-session-jdbc` |
| 2 | Boot 4 默认 Jackson 3（包名 tools.jackson） | 注入旧 ObjectMapper 报「Bean 不存在」 | 全线改用 `tools.jackson.databind.ObjectMapper` |
| 3 | Boot 4 删除了 spring.session.store-type/cookie-name | Cookie 名一直是默认 SESSION | cookie 名改走 `server.servlet.session.cookie.name` |
| 4 | Spring Security 7 常量改名 | 编译报「找不到 SPRING_SECURITY_CONTEXT」 | 改用 `SPRING_SECURITY_CONTEXT_KEY` |
| 5 | 测试注解移包 | @AutoConfigureMockMvc 编译失败 | Boot 4 用 `spring-boot-starter-webmvc-test` + 新包名 |
| 6 | Lombok 与最新 JDK | 能否用要先验证 | 先写最小工程编译实测（D7：1.18.48×JDK25 通过再全量引入） |

## B. 数据库方言类（PostgreSQL）

| # | 坑 | 现象 | 解法 |
| --- | --- | --- | --- |
| 7 | PG enum 列拒绝字符串参数 | `column "kind" is of type spike_kind but expression is character varying` | JDBC URL 加 `stringtype=unspecified` |
| 8 | timestamptz 读不进 LocalDateTime | 写入成功、查询抛 `Cannot convert TIMESTAMPTZ` | 实体时间字段一律 `OffsetDateTime` |
| 9 | 框架 LIKE 大小写敏感 | 搜「invoice」漏掉「Invoice」 | 中文/模糊检索用原生 ILIKE（@Select） |
| 10 | ORM 更新默认忽略 null 字段 | 「恢复=把 deleted_at 置空」根本没写进库 | 用 `update(entity, ignoreNulls=false)` 显式写 null |
| 11 | ORM 插入把 null 显式写入列 | 建了表默认值却不生效，NOT NULL 报错 | 主键/审计字段在代码里显式赋值 |

## C. 业务与事务类

| # | 坑 | 现象 | 解法 |
| --- | --- | --- | --- |
| 12 | 事务回滚卷走业务记录 | 登录失败计数永远不累计、锁定永不生效 | 计数更新移出事务（或独立事务提交）再抛异常 |
| 13 | 校验顺序错位 | 第 4 个附件的「假图」报的是数量超限而非格式错误 | 按 spec 顺序：格式→大小→数量 |
| 14 | 附件超限走 500 | 10MB+1 字节上传返回 500 而非 422 ATT_001 | 专门捕获 `MaxUploadSizeExceededException` 映射错误码 |

## D. 前后端协作类

| # | 坑 | 现象 | 解法 |
| --- | --- | --- | --- |
| 15 | 请求路径双前缀 `/api/v1/api/v1/...` | 前端全部 401（路径没匹配到放行清单） | 生成代码 URL 已含前缀，mutator 不再拼；**E2E 才能发现** |
| 16 | 契约 schema 同名覆盖 | 删除 spike 模块后发票的 CreateRequest 字段丢失 | 唯一命名 + 防线三（前端类型对齐）当场拦截 |
| 17 | 工具把 multipart 生成 JSON | 上传附件 415/400 | 单点手写 FormData 逃生口 + 显式标注待办 |
| 18 | 导出契约抓到僵尸进程 | 8080 上跑着旧版本，导出的接口清单缺新模块 | 导出脚本先清端口占用；产物要抽查 paths 数量 |

## E. 测试工程类

| # | 坑 | 现象 | 解法 |
| --- | --- | --- | --- |
| 19 | MockMvc 下 Spring Session 重复插行 | SPRING_SESSION 主键冲突 | 集成测试改走真实 HTTP（RANDOM_PORT+CookieManager），顺便更接近真实 |
| 20 | 响应式表格/卡片双渲染 | 测试脚本 strict mode 匹配到两个元素 | 断言按视口分派（移动查卡片、桌面查单元格） |
| 21 | 并行测试账号撞号 | 毫秒时间戳相同 → 409 已注册 | 账号加随机后缀 |
| 22 | 测试日期用了「未来的今天」 | 月末跑测试时 17 号之后的日期触发「不能晚于今天」 | 种子数据用上月固定日期 |

## F. 环境/工具类

| # | 坑 | 现象 | 解法 |
| --- | --- | --- | --- |
| 23 | pnpm 被用户主目录的 workspace 吸收 | 依赖装错位置、tsc 找不到 | 工程根放 `pnpm-workspace.yaml` 声明边界 |
| 24 | Docker Hub 拉取慢 | postgres:16-alpine 拉不动 | dev 用本地已有镜像替代（生产不变），记录偏差 |
| 25 | Caddyfile 单行块语法 | `request_body { max_size 15MB }` 报错 | 块必须多行书写；改完要实测反代与屏蔽是否生效 |

## 使用建议

- 遇到报错先扫本表「现象」列——大概率有人踩过；
- 每个解法在仓库里都有对应提交（`git log --oneline` 可查当时怎么修的）；
- **新坑解决后请仿照此表追加记录**——踩坑记录是团队最值钱的文档。
