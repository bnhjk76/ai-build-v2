import { test, expect } from '@playwright/test'
import { seedAccount, createInvoiceVia, loginUi } from './helpers'

test('R5 两级删除文案不同 + 恢复回原位 + 彻底删除', async ({ page }) => {
  const { ctx, account } = await seedAccount()
  const month = new Date().toISOString().slice(0, 7)
  const inv = await createInvoiceVia(ctx, '5151000022223331', `${month}-08`, '回收站E2E抬头', '600.00', '36.00')
  await loginUi(page, account)

  // 第一级：详情页「移入回收站」
  await page.goto(`/invoices/${inv.id}`)
  await page.getByRole('button', { name: '删除（移入回收站）' }).click()
  await expect(page.getByText('将移入回收站，30 天内可恢复')).toBeVisible()
  await page.getByRole('button', { name: '移入回收站', exact: true }).click()
  await expect(page).toHaveURL(/\/invoices$/)

  // 回收站：剩余天数可见
  await page.goto('/recycle')
  await expect(page.getByText('回收站E2E抬头')).toBeVisible()
  await expect(page.getByText(/剩余 \d+ 天/)).toBeVisible()

  // 恢复 → 回到票夹
  await page.getByRole('button', { name: '恢复' }).click()
  await expect(page.getByText('已恢复到票夹')).toBeVisible()
  await page.goto('/invoices')
  await expect(page.getByRole('cell', { name: '回收站E2E抬头' }).or(page.locator('main span').filter({ hasText: '回收站E2E抬头' })).first()).toBeVisible()

  // 第二级：彻底删除（文案刻意不同）
  await page.goto(`/invoices/${inv.id}`)
  await page.getByRole('button', { name: '删除（移入回收站）' }).click()
  await page.getByRole('button', { name: '移入回收站', exact: true }).click()
  await page.goto('/recycle')
  await page.getByRole('button', { name: '彻底删除' }).click()
  await expect(page.getByText('将被永久删除，含全部附件，不可恢复')).toBeVisible()
  await page.getByRole('button', { name: '永久删除' }).click()
  await expect(page.getByText('回收站是空的')).toBeVisible()
})
