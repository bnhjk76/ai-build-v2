import type { ReactNode } from 'react'
import { Button } from './Button'

/** 不可逆/高影响动作的二次确认（DP4：写明后果，不自动消失） */
export function ConfirmDialog({
  open, title, body, confirmText = '确认', danger = true, onConfirm, onCancel,
}: { open: boolean; title: string; body: ReactNode; confirmText?: string; danger?: boolean; onConfirm: () => void; onCancel: () => void }) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" role="dialog" aria-modal>
      <div className="w-[min(92vw,420px)] bg-white rounded-[var(--radius-xl)] p-[var(--space-5)]">
        <h3 className="text-[var(--text-lg)] font-semibold mb-2">{title}</h3>
        <div className="text-[var(--text-sm)] text-[var(--color-ink-700)] mb-5">{body}</div>
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onCancel}>取消</Button>
          <Button variant={danger ? 'danger' : 'primary'} onClick={onConfirm}>{confirmText}</Button>
        </div>
      </div>
    </div>
  )
}
