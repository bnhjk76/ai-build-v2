import type { ReactNode } from 'react'

/** 空态双型（design：首录引导型 vs 筛选无果型） */
export function EmptyState({ type, title, hint, action }: { type: 'first-use' | 'no-result'; title: string; hint?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <div className={`w-12 h-12 rounded-full mb-4 flex items-center justify-center text-2xl
        ${type === 'first-use' ? 'bg-[var(--color-primary-50)]' : 'bg-[var(--color-neutral-50)]'}`}>
        {type === 'first-use' ? '🧾' : '🔍'}
      </div>
      <p className="text-[var(--text-base)] font-medium text-[var(--color-ink-900)]">{title}</p>
      {hint && <p className="mt-1 text-[var(--text-sm)] text-[var(--color-ink-500)]">{hint}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
