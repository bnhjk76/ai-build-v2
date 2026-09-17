import { test, expect } from '@playwright/test'
import { seedAccount, createInvoiceVia } from './helpers'

test('R6 导出：文件名/BOM 首字节/行数（含表头）', async ({ page }) => {
  const { ctx, account } = await seedAccount()
  const month = new Date().toISOString().slice(0, 7)
  await createInvoiceVia(ctx, '5151000022223341', `${month}-09`, '导出E2E一号', '300.00', '18.00')
  await createInvoiceVia(ctx, '5151000022223342', `${month}-10`, '导出E2E二号', '100.00', '6.00')

  const res = await ctx.get('/api/v1/invoices/export')
  expect(res.status()).toBe(200)
  const disposition = res.headers()['content-disposition'] ?? ''
  expect(disposition).toContain('%E7%A5%A8%E5%A4%B9%E9%80%9A')   // UTF-8 编码的「票夹通」
  expect(disposition).toContain('.csv')
  const body = Buffer.from(await res.body())
  expect(body[0]).toBe(0xEF); expect(body[1]).toBe(0xBB); expect(body[2]).toBe(0xBF)   // BOM
  const csv = body.subarray(3).toString('utf-8')
  expect(csv.split('\n').filter((l) => l.trim()).length).toBe(3)   // 表头 + 2 行
  expect(csv).toContain('导出E2E一号')
  expect(csv).toContain('开票日期,发票代码,发票号码')

  // UI：列表页导出按钮存在且携带筛选
  await page.goto('/login')
  await page.getByPlaceholder('邮箱或手机号').fill(account)
  await page.getByPlaceholder('密码').fill('Passw0rd!8')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/invoices$/)
  await expect(page.getByRole('link', { name: '⬇ 导出 CSV' })).toHaveAttribute('href', /\/api\/v1\/invoices\/export/)
})

test.skip('R6 >5000 条预检弹窗不下载', async () => {
  test.skip(true, '需 5001 条种子；后端 EXP_001 逻辑已由 422 语义覆盖，满量用例排 CI 阶段')
})
