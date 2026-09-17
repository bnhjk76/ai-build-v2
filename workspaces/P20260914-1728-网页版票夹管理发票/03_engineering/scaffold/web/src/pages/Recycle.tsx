import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import Decimal from 'decimal.js'
import { listRecycle, purgeRecycleItem, restoreRecycleItem } from '../api/generated/recycle/recycle'
import { unwrap } from '../api/helpers'
import type { RecycleItemView } from '../api/generated/model'
import { Button } from '../components/Button'
import { Card } from '../components/Card'
import { Skeleton } from '../components/Skeleton'
import { EmptyState } from '../components/EmptyState'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useToast } from '../components/Toast'

type RecycleRow = { invoice: NonNullable<RecycleItemView['invoice']>; daysLeft: number }
type RecyclePage = { records: RecycleRow[]; totalRow: number }

/** 回收站（F06/F12，design：两级删除文案刻意区分；剩余 ≤7 天警示色）。 */
export default function RecyclePage() {
  const qc = useQueryClient()
  const { toast } = useToast()
  const [purging, setPurging] = useState<RecycleItemView | null>(null)
  const query = useQuery({ queryKey: ['recycle'], queryFn: () => unwrap<RecyclePage>(listRecycle({ page: 1, page_size: 50 })) })
  const rows = (query.data?.records ?? []).filter((r): r is RecycleRow => Boolean(r.invoice))

  return (
    <div>
      <h1 className="text-[var(--text-xl)] font-semibold mb-4">回收站</h1>
      {query.isLoading ? <Card><Skeleton className="h-16 w-full mb-2" /><Skeleton className="h-16 w-full" /></Card>
        : rows.length === 0 ? <Card><EmptyState type="no-result" title="回收站是空的" hint="删除的发票会在这里保留 30 天" /></Card>
        : (
        <div className="flex flex-col gap-3">
          {rows.map((r) => (
            <Card key={r.invoice.id} className="py-3">
              <div className="flex justify-between items-baseline">
                <span className="font-medium truncate">{r.invoice.title}</span>
                <span className="tabular-nums font-medium">¥{new Decimal(r.invoice.totalAmount ?? '0').toFixed(2)}</span>
              </div>
              <div className="flex justify-between items-center mt-1 text-xs text-[var(--color-ink-500)]">
                <span>{r.invoice.issuedDate} · {r.invoice.invoiceNumber}</span>
                <span className={r.daysLeft <= 7 ? 'text-[var(--color-warning-600)] font-medium' : ''}>
                  剩余 {r.daysLeft} 天
                </span>
              </div>
              <div className="flex gap-2 justify-end mt-2">
                <Button variant="ghost" onClick={async () => {
                  if (r.invoice.id) await restoreRecycleItem(r.invoice.id)
                  toast('已恢复到票夹')
                  qc.invalidateQueries({ queryKey: ['recycle'] }); qc.invalidateQueries({ queryKey: ['invoices'] })
                }}>恢复</Button>
                <Button variant="danger" onClick={() => setPurging(r)}>彻底删除</Button>
              </div>
            </Card>
          ))}
        </div>
      )}
      <ConfirmDialog
        open={purging != null}
        title="彻底删除"
        body={`「${purging?.invoice?.title}」将被永久删除，含全部附件，不可恢复。`}
        confirmText="永久删除"
        onCancel={() => setPurging(null)}
        onConfirm={async () => {
          if (purging?.invoice?.id) await purgeRecycleItem(purging.invoice.id)
          setPurging(null)
          toast('已彻底删除')
          qc.invalidateQueries({ queryKey: ['recycle'] })
        }}
      />
    </div>
  )
}
