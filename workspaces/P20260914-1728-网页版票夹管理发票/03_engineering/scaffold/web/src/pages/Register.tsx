import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '../components/Button'
import { Input } from '../components/Input'
import { Card } from '../components/Card'
import { ApiError } from '../api/client'
import { register as registerApi } from '../api/generated/auth/auth'

interface FormValues { account: string; password: string; confirm: string }

const ACCOUNT_RE = /^([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}|1\d{10})$/

export default function RegisterPage() {
  const navigate = useNavigate()
  const [serverError, setServerError] = useState('')
  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm<FormValues>()

  const onSubmit = async (values: FormValues) => {
    setServerError('')
    try {
      await registerApi({ account: values.account, password: values.password })  // 注册即自动登录
      navigate('/invoices', { replace: true })
    } catch (e) {
      const err = e as ApiError
      if (err.code === 'AUTH_004') setServerError('该邮箱/手机号已注册，请直接登录')
      else setServerError(err.message || '注册失败，请稍后重试')
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <Card className="w-full max-w-sm">
        <h1 className="text-[var(--text-xl)] font-semibold text-center">创建票夹</h1>
        <p className="text-center text-[var(--text-sm)] text-[var(--color-ink-500)] mt-1 mb-6">注册即登录，2 分钟录入第一张发票</p>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
          <Input type="text" placeholder="邮箱或手机号" autoComplete="username"
            error={errors.account?.message}
            {...register('account', { required: '请输入账号', pattern: { value: ACCOUNT_RE, message: '请输入有效的邮箱或 11 位手机号' } })} />
          <Input type="password" placeholder="密码（8–72 位）" autoComplete="new-password"
            error={errors.password?.message}
            {...register('password', { required: '请输入密码', minLength: { value: 8, message: '密码长度需为 8–72 位' }, maxLength: { value: 72, message: '密码长度需为 8–72 位' } })} />
          <Input type="password" placeholder="再次输入密码" autoComplete="new-password"
            error={errors.confirm?.message}
            {...register('confirm', { required: '请再次输入密码', validate: (v) => v === watch('password') || '两次输入的密码不一致' })} />
          {serverError && <p className="text-[var(--text-sm)] text-[var(--color-danger-600)]" role="alert">{serverError}</p>}
          <Button type="submit" loading={isSubmitting} className="w-full">注册并进入</Button>
        </form>
        <p className="text-center mt-4 text-[var(--text-sm)]">
          已有账号？<Link className="text-[var(--color-primary-500)]" to="/login">登录</Link>
        </p>
      </Card>
    </div>
  )
}
