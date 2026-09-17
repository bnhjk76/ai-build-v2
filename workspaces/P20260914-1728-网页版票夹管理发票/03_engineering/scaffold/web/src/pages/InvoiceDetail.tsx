import { Link, useParams } from 'react-router-dom'
import Decimal from 'decimal.js'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { getInvoiceDetail, deleteInvoice } from '../api/generated/invoices/invoices'
import { listAttachments, deleteAttachment } from '../api/generated/attachments/attachments'
import { uploadAttachmentMultipart } from '../api/attachments-multipart'
import { unwrap } from '../api/helpers'
import type { AttachmentView, InvoiceView } from '../api/generated/model'
import { Card } from '../components/Card'
import { Button } from '../components/Button'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useToast } from '../components/Toast'
import { Skeleton } from '../components/Skeleton'

const STATUS_LABEL = { normal: '正常', voided: '作废', reversed: '红冲' } as const

/** 详情页（F07）：完整字段、不脱敏（详情页口径，design §6 脱敏矩阵）。 */
export default function InvoiceDetailPage() {
  const { id = '' } = useParams()
  const qc = useQueryClient()
  const { toast } = useToast()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [uploading, setUploading] = useState(false)
  const query = useQuery({ queryKey: ['invoice', id], queryFn: () => unwrap<InvoiceView>(getInvoiceDetail(id)) })
  const atts = useQuery({ queryKey: ['attachments', id], queryFn: () => unwrap<AttachmentView[]>(listAttachments(id)) })
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

      <h2 className="text-[var(--text-lg)] font-medium mt-6 mb-3">附件（{(atts.data ?? []).length}/3）</h2>
      <Card>
        {(atts.data ?? []).map((a) => (
          <div key={a.id} className="flex justify-between items-center py-2 border-b border-[var(--color-line-light)] last:border-0">
            <div className="min-w-0">
              <a className="text-[var(--color-primary-500)] text-[var(--text-sm)] truncate block"
                 href={`/api/v1/attachments/${a.id}/file`} target="_blank" rel="noreferrer">{a.filename}</a>
              <span className="text-xs text-[var(--color-ink-500)]">{a.mimeType} · {((a.size ?? 0) / 1024).toFixed(0)}KB</span>
            </div>
            <Button variant="ghost" onClick={async () => {
              if (a.id) await deleteAttachment(a.id)
              qc.invalidateQueries({ queryKey: ['attachments', id] })
            }}>删除</Button>
          </div>
        ))}
        <label className={`mt-2 inline-flex items-center justify-center h-10 px-4 rounded-[var(--radius-md)] text-[var(--text-sm)] cursor-pointer
          ${(atts.data ?? []).length >= 3 ? 'opacity-50 pointer-events-none' : 'bg-[var(--color-primary-500)] text-white'}`}>
          {uploading ? '上传中…' : '＋ 上传附件（≤10MB，jpg/png/webp/pdf）'}
          <input type="file" className="hidden" accept=".jpg,.jpeg,.png,.webp,.pdf" disabled={(atts.data ?? []).length >= 3}
            onChange={async (e) => {
              const f = e.target.files?.[0]; if (!f) return
              setUploading(true)
              try {
                await uploadAttachmentMultipart(id, f)
                toast('附件已上传')
                qc.invalidateQueries({ queryKey: ['attachments', id] })
              } catch (err) {
                toast((err as Error).message || '上传失败', 'error')
              } finally { setUploading(false); e.target.value = '' }
            }} />
        </label>
      </Card>

      <div className="flex justify-end mt-6">
        <Button variant="danger" onClick={() => setConfirmDelete(true)}>删除（移入回收站）</Button>
      </div>
      <ConfirmDialog open={confirmDelete} title="删除发票"
        body={`「${inv.title}」将移入回收站，30 天内可恢复（附件随行保留）。`}
        confirmText="移入回收站" onCancel={() => setConfirmDelete(false)}
        onConfirm={async () => { await deleteInvoice(id); window.location.href = '/invoices' }} />
    </div>
  )
}
