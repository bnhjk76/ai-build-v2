import { z } from 'zod'

/**
 * 录入表单 zod schema（tech-stack §4：手写 schema 承载中文行内文案与「≤今天」即时规则，
 * 字段名与生成类型 CreateRequest 对齐——防线三）。
 */
const money = z.string().regex(/^\d{1,10}\.\d{2}$/, '金额需为两位小数，如 3000.00')

export const invoiceFormSchema = z.object({
  invoiceCode: z.string().max(20, '发票代码不能超过 20 位').regex(/^[0-9A-Za-z]*$/, '发票代码仅限数字/字母').optional().or(z.literal('')),
  invoiceNumber: z.string().regex(/^\d{8,20}$/, '发票号码需为 8–20 位数字'),
  issuedDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, '开票日期格式 YYYY-MM-DD')
    .refine((d) => d <= new Date().toISOString().slice(0, 10), '开票日期不能晚于今天'),
  title: z.string().trim().min(1, '请输入发票抬头').max(100, '抬头不能超过 100 字'),
  amount: money.refine((v) => Number(v) > 0, '金额必须大于 0'),
  taxAmount: money.refine((v) => Number(v) >= 0, '税额不能为负数'),
  category: z.enum(['special', 'general']),
  medium: z.enum(['electronic', 'paper']),
  status: z.enum(['normal', 'voided', 'reversed']),
  remark: z.string().max(200, '备注不能超过 200 字').optional().or(z.literal('')),
})

export type InvoiceFormValues = z.infer<typeof invoiceFormSchema>
