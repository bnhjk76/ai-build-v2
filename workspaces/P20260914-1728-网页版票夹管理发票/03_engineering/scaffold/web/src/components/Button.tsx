import type { ButtonHTMLAttributes } from 'react'

export function Button({
  variant = 'primary', loading = false, className = '', children, ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'ghost' | 'danger'; loading?: boolean }) {
  const base = 'inline-flex items-center justify-center h-10 px-4 rounded-[var(--radius-md)] text-[var(--text-sm)] font-medium transition-[filter,background] disabled:opacity-50 disabled:cursor-not-allowed'
  const styles = {
    primary: 'bg-[var(--color-primary-500)] text-white hover:bg-[var(--color-primary-600)] active:bg-[var(--color-primary-700)]',
    ghost: 'bg-transparent text-[var(--color-primary-500)] hover:bg-[var(--color-primary-50)]',
    danger: 'bg-[var(--color-danger-600)] text-white hover:opacity-90',
  }[variant]
  return (
    <button className={`${base} ${styles} ${className}`} disabled={loading || rest.disabled} {...rest}>
      {loading ? '处理中…' : children}
    </button>
  )
}
