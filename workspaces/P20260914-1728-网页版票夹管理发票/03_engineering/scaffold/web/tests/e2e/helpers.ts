import { request, expect, type APIRequestContext, type Page } from '@playwright/test'

export const PNG_BYTES = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0])

export function account() {
  return `e2e_${Date.now()}_${Math.random().toString(36).slice(2, 8)}@test.dev`
}

/** API 上下文注册（独立 cookie 罐），返回 {ctx, account}——浏览器随后用 UI 登录同一账号。 */
export async function seedAccount(): Promise<{ ctx: APIRequestContext; account: string }> {
  const ctx = await request.newContext({ baseURL: 'http://localhost:5173' })
  const acc = account()
  const res = await ctx.post('/api/v1/auth/register', {
    headers: { 'X-Requested-With': 'XMLHttpRequest' },
    data: { account: acc, password: 'Passw0rd!8' },
  })
  expect(res.status()).toBe(201)
  return { ctx, account: acc }
}

export async function createInvoiceVia(ctx: APIRequestContext, number: string, date: string,
                                       title: string, amount: string, tax: string, status?: string) {
  const res = await ctx.post('/api/v1/invoices', {
    headers: { 'X-Requested-With': 'XMLHttpRequest' },
    data: { invoiceNumber: number, issuedDate: date, title, amount, taxAmount: tax,
            category: 'general', medium: 'electronic', ...(status ? { status } : {}) },
  })
  expect(res.status()).toBe(201)
  return (await res.json()).data as { id: string }
}

/** 浏览器 UI 登录指定账号。 */
export async function loginUi(page: Page, account: string) {
  await page.goto('/login')
  await page.getByPlaceholder('邮箱或手机号').fill(account)
  await page.getByPlaceholder('密码').fill('Passw0rd!8')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/invoices$/)
}
