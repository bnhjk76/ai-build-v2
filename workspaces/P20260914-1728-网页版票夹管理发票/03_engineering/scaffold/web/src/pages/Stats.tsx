import { useQuery } from '@tanstack/react-query'
import Decimal from 'decimal.js'
import { getStatsSummary } from '../api/generated/stats/stats'
import { unwrap } from '../api/helpers'
import type { Summary } from '../api/generated/model'
import { Card } from '../components/Card'
import { Skeleton } from '../components/Skeleton'

/** 汇总页（F09/G3，DP2 口径透明：红冲负冲减、作废不计金额的图例说明）。 */
const fmt = (v?: string) => new Decimal(v ?? '0').toFixed(2)

export default function StatsPage() {
  const query = useQuery({ queryKey: ['stats'], queryFn: () => unwrap<Summary>(getStatsSummary()) })
  const s = query.data

  if (query.isLoading) return <Card><Skeleton className="h-24 w-full mb-3" /><Skeleton className="h-24 w-full" /></Card>
  if (!s) return <Card>汇总暂不可用</Card>

  const card = 'bg-[var(--color-primary-50)] rounded-[var(--radius-xl)] p-4'
  const Period = ({ title, p }: { title: string; p: NonNullable<Summary['month']> }) => (
    <div className={card}>
      <div className="text-[var(--text-sm)] text-[var(--color-ink-700)] mb-2">{title}</div>
      <div className={`text-2xl font-semibold tabular-nums ${new Decimal(p.validAmount ?? '0').isNeg() ? 'text-[var(--color-danger-600)]' : ''}`}>
        ¥{fmt(p.validAmount)}
      </div>
      <div className="text-xs text-[var(--color-ink-500)] mt-1">
        有效 {p.validCount ?? 0} 张 · 作废 {p.voidedCount ?? 0} 张（红冲 {p.reversedCount ?? 0} 张已冲减）
      </div>
    </div>
  )

  const dist = s.distribution ?? []
  const maxAmount = Math.max(...dist.map((d) => Number(d.amount ?? 0)), 1)
  const distLabel = { special: '专票', general: '普票' } as const
  const medLabel = { electronic: '电子', paper: '纸质' } as const

  return (
    <div>
      <h1 className="text-[var(--text-xl)] font-semibold mb-4">汇总统计</h1>
      <div className="grid md:grid-cols-2 gap-4 mb-4">
        <Period title="本月" p={s.month!} />
        <Period title="本年" p={s.year!} />
      </div>
      <Card>
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-[var(--text-base)] font-medium">票种 × 介质分布（本年）</h2>
          <button className="text-xs text-[var(--color-info-600)]" title="口径说明"
            onClick={() => alert('口径说明：\n有效金额 = Σ正常 − Σ红冲（红冲为负冲减，可为负数）\n有效张数 = 正常 + 红冲\n作废只计张数，不计入金额')}>ⓘ 口径说明</button>
        </div>
        {dist.length === 0 ? (
          <p className="text-[var(--text-sm)] text-[var(--color-ink-500)]">暂无数据</p>
        ) : dist.map((d, i) => (
          <div key={i} className="mb-2">
            <div className="flex justify-between text-[var(--text-sm)] mb-1">
              <span>{distLabel[d.category as 'special' | 'general']}{medLabel[d.medium as 'electronic' | 'paper']} · {d.count} 张</span>
              <span className="tabular-nums">¥{fmt(d.amount)}</span>
            </div>
            <div className="h-2 rounded-[var(--radius-full)] bg-[var(--color-line-light)]">
              <div className="h-2 rounded-[var(--radius-full)] bg-[var(--color-primary-500)]"
                style={{ width: `${Math.max((Number(d.amount) / maxAmount) * 100, 2)}%` }} />
            </div>
          </div>
        ))}
      </Card>
    </div>
  )
}
