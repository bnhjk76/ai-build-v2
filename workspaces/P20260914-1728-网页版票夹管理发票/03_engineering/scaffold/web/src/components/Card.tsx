import type { HTMLAttributes } from 'react'

export function Card({ className = '', children, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={`bg-[var(--color-bg-card)] rounded-[var(--radius-lg)] border border-[var(--color-line-light)] p-[var(--space-5)] ${className}`} {...rest}>
      {children}
    </div>
  )
}
