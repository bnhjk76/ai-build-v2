import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'

/** 反馈通道（interactions §2.1：成功 4s / 错误 8s） */
const ToastCtx = createContext<{ toast: (message: string, type?: 'success' | 'error') => void }>({ toast: () => {} })
export const useToast = () => useContext(ToastCtx)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<Array<{ id: number; message: string; type: 'success' | 'error' }>>([])
  const toast = useCallback((message: string, type: 'success' | 'error' = 'success') => {
    const id = Date.now() + Math.random()
    setItems((prev) => [...prev, { id, message, type }])
    setTimeout(() => setItems((prev) => prev.filter((t) => t.id !== id)), type === 'success' ? 4000 : 8000)
  }, [])
  return (
    <ToastCtx.Provider value={{ toast }}>
      {children}
      <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 flex flex-col gap-2">
        {items.map((t) => (
          <div key={t.id} className={`px-4 py-2 rounded-[var(--radius-md)] shadow text-[var(--text-sm)] text-white
            ${t.type === 'success' ? 'bg-[var(--color-success-600)]' : 'bg-[var(--color-danger-600)]'}`}>
            {t.message}
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  )
}
