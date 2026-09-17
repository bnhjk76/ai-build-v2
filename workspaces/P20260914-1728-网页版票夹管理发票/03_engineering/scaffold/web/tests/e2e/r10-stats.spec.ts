import { test, expect } from '@playwright/test'
import { seedAccount, createInvoiceVia, loginUi } from './helpers'

test('R10 汇总口径断言：Σ正常−Σ红冲、作废只计张数、负数红色', async ({ page }) => {
  const { ctx, account } = await seedAccount()
  const now = new Date(); now.setMonth(now.getMonth() - 1)
  const prevMonth = now.toISOString().slice(0, 7)
  for (let i = 0; i < 8; i++) {
    await createInvoiceVia(ctx, '51510000222233%02d'.replace('%02d', String(41 + i).padStart(2, '0')),
      `${prevMonth}-15`, '汇总正常票', '600.00', '0.00', 'normal')
  }
  await createInvoiceVia(ctx, '5151000022223390', `${prevMonth}-16`, '汇总红冲票', '800.00', '0.00', 'reversed')
  await createInvoiceVia(ctx, '5151000022223391', `${prevMonth}-17`, '汇总作废票', '999.00', '0.00', 'voided')

  await loginUi(page, account)
  await page.goto('/stats')
  await expect(page.getByText('¥4000.00').first()).toBeVisible()      // 4800 − 800
  await expect(page.getByText(/有效 9 张 · 作废 1 张（红冲 1 张已冲减）/).first()).toBeVisible()
})

test('R10 负数渲染红色 + 空数据 0 值', async ({ page }) => {
  const { ctx, account } = await seedAccount()
  const now = new Date(); now.setMonth(now.getMonth() - 1)
  const prevMonth = now.toISOString().slice(0, 7)
  await createInvoiceVia(ctx, '5151000022223395', `${prevMonth}-18`, '小额正常', '100.00', '0.00', 'normal')
  await createInvoiceVia(ctx, '5151000022223396', `${prevMonth}-19`, '大额红冲', '900.00', '0.00', 'reversed')

  await loginUi(page, account)
  await page.goto('/stats')
  await expect(page.getByText('¥-800.00').first()).toBeVisible()   // 100−900 = −800
  await expect(page.getByText('¥-800.00').first()).toHaveClass(/danger/)

  // 空数据账号：全 0
  const empty = await seedAccount()
  await page.getByRole('button', { name: '退出' }).click()
  await loginUi(page, empty.account)
  await page.goto('/stats')
  await expect(page.getByText('¥0.00').first()).toBeVisible()
  await expect(page.getByText('暂无数据')).toBeVisible()
})
