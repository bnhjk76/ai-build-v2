import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import Decimal from 'decimal.js'
import { listInvoices } from '../api/generated/invoices/invoices'
import { unwrap } from '../api/helpers'
import type { InvoiceView } from '../api/generated/model'

type InvoicePage = { records: InvoiceView[]; totalRow: number; pageNumber: number; pageSize: number }
import { useQuery } from '@tanstack/react-query'
import { Button } from '../components/Button'
import { Input } from '../components/Input'
import { Card } from '../components/Card'
import { Skeleton } from '../components/Skeleton'
import { EmptyState } from '../components/EmptyState'

/** 列表页（W2）：五维筛选常驻 + URL query 同步 + 眼睛切换 + 空态双型 + 桌面表格/移动卡片。 */
const STATUS_LABEL = { normal: '正常', voided: '作废', reversed: '红冲' } as const
const STATUS_CLASS = {
  normal: 'bg-[var(--color-success-50)] text-[var(--color-success-600)]',
  voided: 'bg-[var(--color-neutral-50)] text-[var(--color-neutral-600)]',
  reversed: 'bg-[var(--color-danger-50)] text-[var(--color-danger-600)]',
} as const

export default function InvoicesPage() {
  const [params, setParams] = useSearchParams()
  const [showSensitive, setShowSensitive] = useState(
    () => localStorage.getItem('pref_show_sensitive') === '1',   // R8：切换记忆
  )

  const f = {
    month: params.get('month') ?? '',
    title: params.get('title') ?? '',
    amountMin: params.get('amountMin') ?? '',
    amountMax: params.get('amountMax') ?? '',
    status: params.get('status') ?? '',
    page: Number(params.get('page') ?? 1),
  }

  const setFilter = (k: string, v: string) => {
    const next = new URLSearchParams(params)
    if (v) next.set(k, v); else next.delete(k)
    if (k !== 'page') next.delete('page')
    setParams(next, { replace: true })
  }

  const query = useQuery({
    queryKey: ['invoices', params.toString(), showSensitive],
    queryFn: () => unwrap<InvoicePage>(listInvoices({
      ...(f.month && { month: f.month }),
      ...(f.title && { title: f.title }),
      ...(f.amountMin && { amountMin: f.amountMin }),
      ...(f.amountMax && { amountMax: f.amountMax }),
      ...(f.status && { status: f.status.split(',') }),
      page: f.page,
      page_size: 20,
      ...(showSensitive && { show_sensitive: 1 }),
    })),
  })
  const page = query.data
  const hasFilters = Boolean(f.month || f.title || f.amountMin || f.amountMax || f.status)

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-[var(--text-xl)] font-semibold">我的票夹</h1>
        <div className="flex items-center gap-2">
          <button
            className="h-10 px-3 rounded-[var(--radius-md)] border border-[var(--color-line)] text-[var(--text-sm)]"
            title="显示/隐藏敏感信息"
            onClick={() => {
              const next = !showSensitive
              setShowSensitive(next)
              localStorage.setItem('pref_show_sensitive', next ? '1' : '0')
            }}
          >{showSensitive ? '🙈 隐藏全号' : '👁 显示全号'}</button>
          <a href={`/api/v1/invoices/export?${params.toString()}`} className="h-10 px-3 flex items-center rounded-[var(--radius-md)] border border-[var(--color-line)] text-[var(--text-sm)]">⬇ 导出 CSV</a>
          <Link to="/invoices/new"><Button>＋ 新增发票</Button></Link>
        </div>
      </div>

      {/* 筛选条常驻（DP1：不折叠丢失） */}
      <Card className="mb-4">
        <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
          <Input type="month" placeholder="月份" defaultValue={f.month}
            onChange={(e) => setFilter('month', e.target.value)} />
          <Input type="text" placeholder="抬头关键字" defaultValue={f.title}
            onChange={(e) => setFilter('title', e.target.value)} />
          <Input type="text" placeholder="金额 ≥" defaultValue={f.amountMin}
            onChange={(e) => setFilter('amountMin', e.target.value)} />
          <Input type="text" placeholder="金额 ≤" defaultValue={f.amountMax}
            onChange={(e) => setFilter('amountMax', e.target.value)} />
          <select className="h-10 px-3 rounded-[var(--radius-md)] border border-[var(--color-line)] bg-white text-[var(--text-sm)]"
            value={f.status} onChange={(e) => setFilter('status', e.target.value)}>
            <option value="">全部状态</option>
            <option value="normal">正常</option>
            <option value="voided">作废</option>
            <option value="reversed">红冲</option>
          </select>
        </div>
      </Card>

      {query.isLoading ? (
        <Card><Skeleton className="h-8 w-full mb-3" /><Skeleton className="h-8 w-full mb-3" /><Skeleton className="h-8 w-2/3" /></Card>
      ) : page == null || page.records?.length === 0 ? (
        <Card>
          {hasFilters
            ? <EmptyState type="no-result" title="没有匹配的发票" hint="试试放宽筛选条件" />
            : <EmptyState type="first-use" title="票夹还是空的" hint="录入第一张发票，2 分钟搞定"
                action={<Link to="/invoices/new"><Button>＋ 新增发票</Button></Link>} />}
        </Card>
      ) : (
        <>
          {/* 桌面表格 */}
          <Card className="hidden md:block overflow-x-auto">
            <table className="w-full text-[var(--text-sm)]">
              <thead>
                <tr className="text-left text-[var(--color-ink-500)] border-b border-[var(--color-line-light)]">
                  <th className="py-2 pr-3 font-normal">开票日期</th>
                  <th className="py-2 pr-3 font-normal">抬头</th>
                  <th className="py-2 pr-3 font-normal">号码</th>
                  <th className="py-2 pr-3 font-normal">类型</th>
                  <th className="py-2 pr-3 font-normal">价税合计</th>
                  <th className="py-2 pr-3 font-normal">状态</th>
                </tr>
              </thead>
              <tbody>
                {page.records.map((inv) => (
                  <tr key={inv.id} className="border-b border-[var(--color-line-light)] last:border-0 hover:bg-[var(--color-primary-50)]">
                    <td className="py-2.5 pr-3"><Link className="text-[var(--color-primary-500)]" to={`/invoices/${inv.id}`}>{inv.issuedDate}</Link></td>
                    <td className="py-2.5 pr-3 max-w-56 truncate">{inv.title}</td>
                    <td className="py-2.5 pr-3 tabular-nums">{inv.invoiceNumber}</td>
                    <td className="py-2.5 pr-3">{inv.medium === 'electronic' ? '电子' : '纸质'}{inv.category === 'special' ? '专票' : '普票'}</td>
                    <td className="py-2.5 pr-3 tabular-nums font-medium">¥{new Decimal(inv.totalAmount ?? '0').toFixed(2)}</td>
                    <td className="py-2.5 pr-3"><span className={`inline-block px-2 h-[22px] leading-[22px] rounded-[var(--radius-full)] text-xs ${STATUS_CLASS[inv.status as keyof typeof STATUS_CLASS]}`}>{STATUS_LABEL[inv.status as keyof typeof STATUS_LABEL]}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>

          {/* 移动卡片 */}
          <div className="md:hidden flex flex-col gap-3">
            {page.records.map((inv) => (
              <Card key={inv.id} className="py-3">
                <Link to={`/invoices/${inv.id}`} className="flex justify-between items-baseline">
                  <span className="text-[var(--text-base)] font-medium truncate">{inv.title}</span>
                  <span className="tabular-nums font-medium">¥{new Decimal(inv.totalAmount ?? '0').toFixed(2)}</span>
                </Link>
                <div className="flex justify-between mt-1 text-xs text-[var(--color-ink-500)]">
                  <span>{inv.issuedDate} · {inv.invoiceNumber}</span>
                  <span className={`px-2 h-[22px] leading-[22px] rounded-[var(--radius-full)] ${STATUS_CLASS[inv.status as keyof typeof STATUS_CLASS]}`}>{STATUS_LABEL[inv.status as keyof typeof STATUS_LABEL]}</span>
                </div>
              </Card>
            ))}
          </div>

          {/* 分页（条件保持于 URL，翻页不丢上下文） */}
          <div className="flex items-center justify-between mt-4 text-[var(--text-sm)] text-[var(--color-ink-500)]">
            <span>共 {page.totalRow} 条 · 第 {page.pageNumber} 页</span>
            <div className="flex gap-2">
              <Button variant="ghost" disabled={f.page <= 1} onClick={() => setFilter('page', String(f.page - 1))}>上一页</Button>
              <Button variant="ghost" disabled={f.page * 20 >= page.totalRow} onClick={() => setFilter('page', String(f.page + 1))}>下一页</Button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
