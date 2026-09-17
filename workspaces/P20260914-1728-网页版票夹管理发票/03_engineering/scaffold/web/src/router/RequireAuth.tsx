import { useEffect, useState, type ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { me } from '../api/generated/auth/auth'
import { Card } from '../components/Card'

/** 登录态守卫：/auth/me 探测会话（IA §6 权限矩阵：未登录 302 登录后回跳） */
export function RequireAuth({ children }: { children: ReactNode }) {
  const location = useLocation()
  const [state, setState] = useState<'loading' | 'ok' | 'anon'>('loading')

  useEffect(() => {
    let alive = true
    me().then(() => alive && setState('ok')).catch(() => alive && setState('anon'))
    return () => { alive = false }
  }, [])

  if (state === 'loading') {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Card>正在验证登录状态…</Card>
      </div>
    )
  }
  if (state === 'anon') {
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />
  }
  return <>{children}</>
}
