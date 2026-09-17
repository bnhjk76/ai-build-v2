
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { logout } from '../api/generated/auth/auth'
import { me } from '../api/generated/auth/auth'
import { useEffect, useState } from 'react'

/** 顶栏布局壳：一级导航 3 项（IA §3；移动底部 Tab 列 M2）。退出入口一级可达（DP3）。 */
export function AppShell() {
  const navigate = useNavigate()
  const [maskedAccount, setMaskedAccount] = useState('')

  useEffect(() => {
    me().then((r) => setMaskedAccount((r as unknown as { account?: string }).account ?? '')).catch(() => {})
  }, [])

  const navItem = 'px-3 py-1.5 rounded-[var(--radius-full)] text-[var(--text-sm)] transition-colors'

  return (
    <div className="min-h-screen">
      <header className="bg-white border-b border-[var(--color-line-light)] sticky top-0 z-40">
        <div className="max-w-5xl mx-auto px-4 h-14 flex items-center gap-4">
          <Link to="/invoices" className="font-semibold text-[var(--text-lg)]">票夹通</Link>
          <nav className="flex gap-1">
            <NavLink to="/invoices" className={({ isActive }) => `${navItem} ${isActive ? 'bg-[var(--color-primary-100)] text-[var(--color-primary-700)]' : 'text-[var(--color-ink-700)]'}`}>票夹</NavLink>
            <NavLink to="/stats" className={({ isActive }) => `${navItem} ${isActive ? 'bg-[var(--color-primary-100)] text-[var(--color-primary-700)]' : 'text-[var(--color-ink-700)]'}`}>汇总</NavLink>
            <NavLink to="/recycle" className={({ isActive }) => `${navItem} ${isActive ? 'bg-[var(--color-primary-100)] text-[var(--color-primary-700)]' : 'text-[var(--color-ink-700)]'}`}>回收站</NavLink>
          </nav>
          <div className="ml-auto flex items-center gap-3">
            <span className="text-[var(--text-xs)] text-[var(--color-ink-500)]">{maskedAccount}</span>
            <button
              className="text-[var(--text-sm)] text-[var(--color-ink-500)] hover:text-[var(--color-danger-600)]"
              onClick={async () => { try { await logout() } finally { navigate('/login', { replace: true }) } }}
            >退出</button>
          </div>
        </div>
      </header>
      <main className="max-w-5xl mx-auto px-4 py-6"><Outlet /></main>
    </div>
  )
}
