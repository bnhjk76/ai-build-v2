import { forwardRef, type InputHTMLAttributes } from 'react'

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement> & { error?: string }>(
  function Input({ error, className = '', ...rest }, ref) {
    return (
      <label className="block">
        <input
          ref={ref}
          className={`w-full h-10 px-3 rounded-[var(--radius-md)] border bg-white text-[var(--text-base)]
            ${error ? 'border-[var(--color-danger-600)]' : 'border-[var(--color-line)]'}
            focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-500)]/30 focus:border-[var(--color-primary-500)] ${className}`}
          {...rest}
        />
        {error && <span className="block mt-1 text-xs text-[var(--color-danger-600)]">{error}</span>}
      </label>
    )
  },
)
