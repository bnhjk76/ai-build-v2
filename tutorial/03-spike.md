# 第 3 章 技术选型与「用实验消灭不确定性」

> 学习线：☕ Java ｜ 对应真实文件：`WS/03_engineering/tech-stack.md`、`WS/03_engineering/SPIKE-W1D1.md`

## 3.1 选型不是选「最好的」，是选「最合适的」

本项目后端选了 Spring Boot（Java 最主流框架）+ PostgreSQL（关系型数据库）+ MyBatis-Flex（数据库操作库）。为什么？tech-stack.md 给了 5 条约束，前两条对新手最有价值：

1. **单体优先**：一个后端程序 + 一个数据库就是够，**绝不为了炫技上微服务**——个人项目上微服务就像买菜开卡车；
2. **不确定就实验**：任何没把握的版本组合，先花 2 天做 Spike 验证，再开工。

## 3.2 Spike：一个值得记住的方法论

Spike（技术刺探）= 一个**用完就扔**的最小验证程序。本项目真实案例：新技术组合「Spring Boot 4.1.1 + JDK 25 + MyBatis-Flex」从未被一起验证过，于是第一天做了 5 项冒烟测试：

| 冒烟项 | 验证什么 | 结果 |
| --- | --- | --- |
| 起服务 | 程序能不能跑起来 | ✅ 2.4 秒 |
| 连数据库 | 能不能读写数据 | ✅ |
| 事务 | 出错时数据能不能回滚 | ✅ |
| 分页 | 查询能不能翻页 | ✅ |
| 会话落库 | 登录状态能不能存进数据库 | ✅ |

**2 天实验排除了 5 个风险，还顺手发现 8 个坑**（全部记录在 SPIKE-W1D1.md，第 4-5 章会讲到其中的典型）。
启示：写正式代码前，先用最小实验回答「这条路通不通」，比任何网上攻略都可靠。

## 3.3 项目结构：目录即架构

看 `WS/03_engineering/scaffold/server/`（这就是本项目的 Java 工程根目录）：

```
server/
├── pom.xml            ← 依赖清单（Maven：Java 世界的 npm）
├── src/main/java/com/ticketwallet/
│   ├── TicketWalletApplication.java   ← 程序入口
│   ├── common/        ← 横切能力（错误码/安全/存储）
│   │   ├── error/     ← 统一错误码（16 个）
│   │   └── security/  ← 安全配置
│   └── domain/        ← 业务模块（一个业务一个包）
│       ├── auth/      ← 登录注册
│       └── invoice/   ← 发票
└── src/main/resources/
    ├── application.yml  ← 配置文件（端口/数据库地址）
    └── db/migration/    ← 数据库建表脚本（Flyway）
```

**给新手的翻译**：Java 项目用「包」组织代码，`com.ticketwallet.domain.invoice` 这个路径就像文件夹一层层套下去；`pom.xml` 声明「我用了哪些第三方库」，Maven 会自动下载。

## 3.4 Maven：第一次接触构建工具

```bash
./mvnw compile   # 编译（mvnw 是 Maven 的自带启动器，保证所有人用同一版本）
./mvnw test      # 跑测试
./mvnw package   # 打包成可运行的 jar
```

本项目 pom.xml 里的关键依赖（依赖 = 别人写好的功能包）：

| 依赖 | 干什么用 | 生活比喻 |
| --- | --- | --- |
| spring-boot-starter-webmvc | 提供 HTTP 接口能力 | 开店的门面 |
| mybatis-flex-spring-boot4-starter | 操作数据库 | 仓库管理员 |
| flyway / spring-boot-starter-flyway | 管理建表脚本版本 | 装修施工队 |
| spring-boot-starter-security | 登录、密码加密 | 保安 |
| lombok | 自动生成 get/set 方法 | 秘书 |

## 练习

1. 打开 `WS/03_engineering/SPIKE-W1D1.md` 第 3 节「新发现」表，找出 F3（stringtype）和 F5（OffsetDateTime）两条，先猜猜是什么问题，再看处置；
2. 假设你要选一个笔记软件框架，列出 3 条你的真实约束（像 3.1 节那样）。
