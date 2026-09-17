# 第 4 章 Java 后端实战：三层结构与数据库

> 学习线：☕ Java ｜ 对应真实文件：`WS/…/scaffold/server/src/main/java/com/ticketwallet/domain/invoice/`

## 4.1 一个请求的旅程（三层结构）

浏览器请求「发票列表」时，后端内部发生什么？以本项目真实代码为例：

```
HTTP GET /api/v1/invoices
   │
   ▼ InvoiceController（控制器层：翻译 HTTP）
   │  接收参数 page=1、校验登录态，调用下一层
   ▼ InvoiceService（服务层：业务规则）
   │  拼查询条件（月份+抬头+金额区间）、算掩码、重算价税合计
   ▼ InvoiceMapper（数据层：数据库操作）
   │  生成 SQL：SELECT … WHERE user_id=? AND deleted_at IS NULL …
   ▼ PostgreSQL 数据库
```

**新手提示**：控制器不写业务、服务层不写 SQL——各层各司其职，代码才不会糊成一锅粥。
对照读：`InvoiceController.java`（约 60 行）→ `InvoiceService.java` → `InvoiceMapper.java`（仅 6 行！）。

## 4.2 实体与数据库表（ORM 入门）

Java 里用「类」描述数据库里的一张表（这个类叫**实体**）：

```java
@Getter @Setter                    // Lombok：自动生成 get/set（D7 决策）
@Table("invoices")                 // 对应 invoices 表
public class Invoice {
    @Id
    private String id;             // 主键
    private String invoiceNumber;  // 发票号码
    private BigDecimal amount;     // 金额（BigDecimal=精确小数，金额永远不用 double！）
    private InvoiceStatus status;  // 状态（枚举：NORMAL/VOIDED/REVERSED）
    private OffsetDateTime createdAt;
}
```

建表不手写 SQL 脚本乱放，而是放进 `db/migration/` 目录由 **Flyway** 统一管理（`V1__spike_init.sql`、`V2__core_tables.sql`…），程序启动时自动执行——这样每台环境的表结构永远一致。

## 4.3 真实案例：五个维度筛选是怎么实现的

需求（F08）：按月份、抬头关键字、金额区间、票种、状态筛选，条件之间「并且」组合。
`InvoiceService.buildFilterWrapper()` 的实现逻辑（简化）：

```java
QueryWrapper qw = QueryWrapper.create()
        .where("user_id = ?", userId)        // 只能看自己的（安全底线！）
        .and("deleted_at IS NULL");          // 回收站里的不出现
if (month != null)  qw.and("issued_date >= ? AND issued_date < ?", ...);
if (kw != null)     qw.and("title ILIKE ?", "%" + kw + "%");  // 模糊搜索
if (min != null)    qw.and("total_amount >= ?", min);
```

为什么这样能快？数据库里预先建好了**索引**（`V2__core_tables.sql` 里那几行 CREATE INDEX），就像书的目录——不翻每一页也能找到内容。本项目实测：1000 条数据五维组合筛选 **P95 = 31 毫秒**（预算 500ms）。

## 4.4 踩坑实录（本项目真实发生，教你两个重要习惯）

**坑 1（数据库方言）**：抬头搜索用 ILIKE（不区分大小写），但框架默认生成的是 LIKE（区分大小写）——搜「科技」搜不到「科技」没问题，搜「invoice」搜不到「Invoice」就是 bug。
👉 习惯：涉及数据库行为，**写个最小用例实测**，别信文档。

**坑 2（金额精度）**：金额一律用 `BigDecimal` + 两位小数字符串传输（`"3000.00"`），因为 `0.1 + 0.2 ≠ 0.3` 是浮点数的物理特性。全项目没有出现过一个 double 金额。

## 练习

1. 跟读一遍 `InvoiceService.create()`：找出「服务端重算价税合计」那行，想想为什么不能信前端传来的合计；
2. 打开 `V2__core_tables.sql`，找到回收站专用索引 `idx_inv_recycle`，对照 4.3 节理解「部分索引」的 WHERE 子句在过滤什么。
