import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import Decimal from 'decimal.js'
import { Button } from '../components/Button'
import { Input } from '../components/Input'
import { Card } from '../components/Card'
import { ApiError } from '../api/client'
import { createInvoice } from '../api/generated/invoices/invoices'
import { invoiceFormSchema, type InvoiceFormValues } from '../lib/schemas/invoice'

const DRAFT_KEY = 'draft_invoice_new'

/** 录入表单（F04，G1）：blur+提交双校验、价税合计实时、断网草稿本地保留（DP4/R3）。 */
export default function InvoiceFormPage() {
  const navigate = useNavigate()
  const [serverError, setServerError] = useState('')
  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } =
    useForm<InvoiceFormValues>({
      resolver: zodResolver(invoiceFormSchema),
      mode: 'onBlur',   // blur 即时校验 + 提交全量校验
      defaultValues: {
        category: 'general', medium: 'electronic', status: 'normal',
        ...(JSON.parse(localStorage.getItem(DRAFT_KEY) ?? '{}') as Partial<InvoiceFormValues>),
      },
    })

  const amount = watch('amount')
  const tax = watch('taxAmount')
  const values = watch()
  useEffect(() => {   // 草稿：随填随存，成功后清除
    localStorage.setItem(DRAFT_KEY, JSON.stringify(values))
  }, [values])

  const total = (() => {
    const a = new Decimal(/^\d/.test(amount ?? '') ? amount! : '0')
    const t = new Decimal(/^\d/.test(tax ?? '') ? tax! : '0')
    return a.plus(t).toFixed(2)
  })()

  const onSubmit = async (v: InvoiceFormValues) => {
    setServerError('')
    try {
      await createInvoice({
        invoiceCode: v.invoiceCode || undefined,
        invoiceNumber: v.invoiceNumber,
        issuedDate: v.issuedDate,
        title: v.title,
        amount: v.amount,
        taxAmount: v.taxAmount,
        category: v.category,
        medium: v.medium,
        status: v.status,
        remark: v.remark || undefined,
      })
      localStorage.removeItem(DRAFT_KEY)
      navigate('/invoices', { replace: true })
    } catch (e) {
      // 提交失败不 reset：表单内容原地保留可重试（US04 异常路径）
      const err = e as ApiError
      setServerError(err.details?.map((d) => d.message).join('；') || err.message)
    }
  }

  const label = 'block text-[var(--text-sm)] text-[var(--color-ink-700)] mb-1'
  return (
    <div className="max-w-xl mx-auto">
      <h1 className="text-[var(--text-xl)] font-semibold mb-4">新增发票</h1>
      <Card>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={label}>发票代码（选填）</label>
              <Input placeholder="数电票可留空" error={errors.invoiceCode?.message} {...register('invoiceCode')} />
            </div>
            <div>
              <label className={label}>发票号码 *</label>
              <Input placeholder="8–20 位数字" inputMode="numeric" error={errors.invoiceNumber?.message} {...register('invoiceNumber')} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={label}>开票日期 *</label>
              <Input type="date" error={errors.issuedDate?.message} {...register('issuedDate')} />
            </div>
            <div>
              <label className={label}>抬头 *</label>
              <Input placeholder="购买方名称" error={errors.title?.message} {...register('title')} />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className={label}>金额（不含税）*</label>
              <Input placeholder="3000.00" inputMode="decimal" error={errors.amount?.message} {...register('amount')} />
            </div>
            <div>
              <label className={label}>税额 *</label>
              <Input placeholder="180.00" inputMode="decimal" error={errors.taxAmount?.message} {...register('taxAmount')} />
            </div>
            <div>
              <label className={label}>价税合计</label>
              <div className="h-10 px-3 flex items-center rounded-[var(--radius-md)] bg-[var(--color-primary-50)] text-[var(--color-primary-700)] font-medium tabular-nums">
                ¥{total}
              </div>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className={label}>票种</label>
              <select className="w-full h-10 px-3 rounded-[var(--radius-md)] border border-[var(--color-line)] bg-white" {...register('category')}>
                <option value="general">普票</option>
                <option value="special">专票</option>
              </select>
            </div>
            <div>
              <label className={label}>介质</label>
              <select className="w-full h-10 px-3 rounded-[var(--radius-md)] border border-[var(--color-line)] bg-white" {...register('medium')}>
                <option value="electronic">电子</option>
                <option value="paper">纸质</option>
              </select>
            </div>
            <div>
              <label className={label}>状态</label>
              <select className="w-full h-10 px-3 rounded-[var(--radius-md)] border border-[var(--color-line)] bg-white" {...register('status')}>
                <option value="normal">正常</option>
                <option value="voided">作废</option>
                <option value="reversed">红冲</option>
              </select>
            </div>
          </div>
          <div>
            <label className={label}>备注</label>
            <Input placeholder="≤200 字" error={errors.remark?.message} {...register('remark')} />
          </div>
          {serverError && <p className="text-[var(--text-sm)] text-[var(--color-danger-600)]" role="alert">{serverError}</p>}
          <div className="flex gap-2 justify-end">
            <Button variant="ghost" type="button" onClick={() => navigate('/invoices')}>取消</Button>
            <Button type="submit" loading={isSubmitting}>保存发票</Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
