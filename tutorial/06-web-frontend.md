# 第 6 章 Web 前端实战：从页面到数据

> 学习线：🌐 Web ｜ 对应真实文件：`WS/…/scaffold/web/src/`

## 6.1 前端项目的骨架

```
web/
├── index.html            ← 唯一的 HTML（SPA：单页应用）
├── src/
│   ├── main.tsx          ← 程序入口
│   ├── router/           ← 路由：URL ↔ 页面 的对应表
│   ├── pages/            ← 页面组件（登录/列表/详情/汇总/回收站）
│   ├── components/       ← 通用零件（按钮/输入框/弹窗…7 个）
│   ├── api/              ← 与后端通信（含自动生成的代码！）
│   └── styles/tokens.css ← 设计变量（第 2 章的 Design Tokens 落地）
```

**新手概念速览**：
- **React**：用「组件」（函数）拼页面，数据变了界面自动更新；
- **TypeScript**：带类型检查的 JavaScript（`invoice.title` 拼错字段名，编译时直接报错而不是运行时白屏）；
- **Vite**：开发服务器+打包器，改代码浏览器秒级热更新。

## 6.2 路由与守卫

`router/index.tsx` 声明 URL 与页面的对应：`/login`→登录页、`/invoices`→票夹……
`RequireAuth` 组件像门卫：进入票夹前先调后端 `/auth/me` 问「他登录了吗」，没登录就踢到 `/login?redirect=原地址`（登录后还能回来）。

## 6.3 表单：录入页的信息密度

`pages/InvoiceForm.tsx` 用 react-hook-form + zod 实现了产品要求的所有细节：
- **blur 即时校验 + 提交全量校验**：光标离开输入框就提示「发票号码需为 8–20 位数字」；
- **价税合计实时计算**：金额、税额一变，右侧只读框立刻用 decimal.js 算出合计；
- **断网草稿**：填一半的内容存 localStorage，失败重试时原样回填。

## 6.4 与后端通信：客户端是「生成」出来的

前端不手写接口调用！后端注解 → 生成 `contracts/openapi.json`（接口契约）→ orval 工具自动生成 TS 类型和调用函数：

```ts
// 这行 import 的文件是机器生成的，改接口必须重新生成，手改会被 CI 拦下
import { createInvoice, listInvoices } from '../api/generated/invoices/invoices'
```

好处：后端改了字段名，前端**编译直接红**，而不是上线后用户看到 undefined。

## 6.5 状态与体验细节（真实代码里的例子）

- **筛选条件进 URL**：`/invoices?month=2026-08&title=科技`——刷新、分享、后退都不丢（见 Invoices.tsx 的 useSearchParams）；
- **空态双型**：没数据时显示「票夹还是空的+新建按钮」，筛选无果时显示「试试放宽条件」——同一个空状态，含义完全不同；
- **敏感信息眼睛切换**：发票号默认 `****5678`，点眼睛显示全号，偏好记在 localStorage。

## 练习

1. 打开 `InvoiceForm.tsx` 找到 zod schema（`lib/schemas/invoice.ts`），对照第 2 章 features.md 的校验矩阵数一数覆盖了几个字段；
2. 改造练习：给列表页加一个「本周」快捷筛选按钮（提示：改 setFilter 和 URL 参数）。
