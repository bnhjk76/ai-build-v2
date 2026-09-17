import { Link, useParams } from 'react-router-dom'
import Decimal from 'decimal.js'
import { useQuery } from '@tanstack/react-query'
import { getInvoiceDetail } from '../api/generated/invoices/invoices'
import type { InvoiceView } from '../api/generated/model'
import { unwrap } from '../api/helpers'
import { Card } from '../components/Card'
import { Skeleton } from '../components/Skeleton'

const STATUS_LABEL = { normal: '正常', voided: '作废', reversed: '红冲' } as const

/** 详情页（F07）：完整字段、不脱敏（详情页口径，design §6 脱敏矩阵）。 */
export default function InvoiceDetailPage() {
  const { id = '' } = useParams()
  const query = useQuery({ queryKey: ['invoice', id], queryFn: () => unwrap<InvoiceView>(getInvoiceDetail(id)) })
  const inv = query.data

  if (query.isLoading) return <Card><Skeleton className="h-6 w-1/3 mb-3" /><Skeleton className="h-4 w-2/3 mb-2" /><Skeleton className="h-4 w-1/2" /></Card>
  if (!inv) return <Card>记录不存在或已删除</Card>

  const row = 'flex justify-between py-2.5 border-b border-[var(--color-line-light)] last:border-0'
  return (
    <div className="max-w-xl mx-auto">
      <Link to="/invoices" className="text-[var(--text-sm)] text-[var(--color-primary-500)]">← 返回票夹</Link>
      <h1 className="text-[var(--text-xl)] font-semibold my-4 truncate">{inv.title}</h1>
      <Card>
        <div className="text-[var(--text-sm)]">
          <div className={row}><span className="text-[var(--color-ink-500)]">开票日期</span><span>{inv.issuedDate}</span></div>
          <div className={row}><span className="text-[var(--color-ink-500)]">发票代码</span><span className="tabular-nums">{inv.invoiceCode ?? '—'}</span></div>
          <div className={row}><span className="text-[var(--color-ink-500)]">发票号码</span><span className="tabular-nums">{inv.invoiceNumber}</span></div>
          <div className={row}><span className="text-[var(--color-ink-500)]">类型</span><span>{inv.medium === 'electronic' ? '电子' : '纸质'}{inv.category === 'special' ? '专票' : '普票'}</span></div>
          <div className={row}><span className="text-[var(--color-ink-500)]">状态</span><span>{STATUS_LABEL[inv.status as keyof typeof STATUS_LABEL]}</span></div>
          <div className={row}><span className="text-[var(--color-ink-500)]">金额（不含税）</span><span className="tabular-nums">¥{new Decimal(inv.amount ?? '0').toFixed(2)}</span></div>
          <div className={row}><span className="text-[var(--color-ink-500)]">税额</span><span className="tabular-nums">¥{new Decimal(inv.taxAmount ?? '0').toFixed(2)}</span></div>
          <div className={`${row} font-medium`}><span>价税合计</span><span className="tabular-nums">¥{new Decimal(inv.totalAmount ?? '0').toFixed(2)}</span></div>
          {inv.remark && <div className={row}><span className="text-[var(--color-ink-500)]">备注</span><span className="text-right max-w-64">{inv.remark}</span></div>}
          <div className={row}><span className="text-[var(--color-ink-500)]">录入时间</span><span>{inv.createdAt?.replace('T', ' ').slice(0, 19)}</span></div>
        </div>
      </Card>
    </div>
  )
}
