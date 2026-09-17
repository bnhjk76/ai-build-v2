import { test, expect } from '@playwright/test'
import { seedAccount, createInvoiceVia, loginUi } from './helpers'
import type { Locator, Page } from '@playwright/test'

function maskedOrFull(page: Page, text: string): Locator {
  const mobile = (page.viewportSize()?.width ?? 1280) < 768
  return mobile
    ? page.locator('main span').filter({ hasText: text }).first()
    : page.getByText(text).first()
}

test('R8 默认掩码/眼睛切换记忆/顶栏账号掩码', async ({ page }) => {
  const { ctx, account } = await seedAccount()
  const month = new Date().toISOString().slice(0, 7)
  const number = '5151000022223361'
  await createInvoiceVia(ctx, number, `${month}-13`, '脱敏E2E抬头', '500.00', '30.00')
  await loginUi(page, account)

  // 顶栏账号掩码（a***@…）
  await expect(page.locator('header span').filter({ hasText: '***' })).toBeVisible()

  // 默认掩码 ****3361
  await expect(maskedOrFull(page, `****${number.slice(-4)}`)).toBeVisible()
  // 切换显示全号（R8 眼睛）
  await page.getByRole('button', { name: /显示全号/ }).click()
  await expect(page.getByRole('button', { name: /隐藏全号/ })).toBeVisible()
  await expect(maskedOrFull(page, number)).toBeVisible()
  // 刷新后记忆（localStorage pref_show_sensitive）
  await page.reload()
  await expect(page.getByRole('button', { name: /隐藏全号/ })).toBeVisible()
  await expect(maskedOrFull(page, number)).toBeVisible()
})
