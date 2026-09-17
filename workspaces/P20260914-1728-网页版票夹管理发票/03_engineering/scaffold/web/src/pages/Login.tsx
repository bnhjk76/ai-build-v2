import { useForm } from 'react-hook-form'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useState } from 'react'
import { Button } from '../components/Button'
import { Input } from '../components/Input'
import { Card } from '../components/Card'
import { ApiError } from '../api/client'
import { login } from '../api/generated/auth/auth'

interface FormValues { account: string; password: string }

export default function LoginPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [serverError, setServerError] = useState('')
  const [retryAfter, setRetryAfter] = useState<number | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>()

  const onSubmit = async (values: FormValues) => {
    setServerError(''); setRetryAfter(null)
    try {
      await login(values)   // 语义：失败统一文案，不泄露哪个字段错（AUTH_001）
      navigate(params.get('redirect') || '/invoices', { replace: true })
    } catch (e) {
      const err = e as ApiError
      if (err.code === 'AUTH_002') {
        const mins = err.details?.find((d) => d.field === 'retryAfterMinutes')?.message
        setRetryAfter(Number(mins ?? 0))
        setServerError(`账号已锁定，请 ${mins ?? '几'} 分钟后重试`)
      } else {
        setServerError(err.message || '账号或密码错误')
      }
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <Card className="w-full max-w-sm">
        <h1 className="text-[var(--text-xl)] font-semibold text-center">票夹通</h1>
        <p className="text-center text-[var(--text-sm)] text-[var(--color-ink-500)] mt-1 mb-6">
          你开过的每一张发票，都在这里
        </p>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
          <Input type="text" placeholder="邮箱或手机号" autoComplete="username"
            error={errors.account?.message}
            {...register('account', { required: '请输入账号' })} />
          <Input type="password" placeholder="密码" autoComplete="current-password"
            error={errors.password?.message}
            {...register('password', { required: '请输入密码' })} />
          {serverError && (
            <p className="text-[var(--text-sm)] text-[var(--color-danger-600)]" role="alert">{serverError}</p>
          )}
          <Button type="submit" loading={isSubmitting} disabled={retryAfter !== null} className="w-full">
            {retryAfter !== null ? `已锁定，剩余约 ${retryAfter} 分钟` : '登录'}
          </Button>
        </form>
        <p className="text-center mt-4 text-[var(--text-sm)]">
          没有账号？<Link className="text-[var(--color-primary-500)]" to="/register">注册</Link>
        </p>
      </Card>
    </div>
  )
}
