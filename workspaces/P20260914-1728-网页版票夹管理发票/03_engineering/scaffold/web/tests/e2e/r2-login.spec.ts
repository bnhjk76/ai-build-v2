import { test, expect } from '@playwright/test'

/** R2 登录：错误统一文案、第 6 次锁定禁用按钮+剩余分钟、redirect 回跳 */
test('R2 密码错误统一文案', async ({ page }) => {
  const account = `e2e_${Date.now()}_${Math.random().toString(36).slice(2, 8)}@test.dev`
  await registerViaUi(page, account)
  await page.getByRole('button', { name: '退出' }).click()
  await expect(page).toHaveURL(/\/login/)

  await page.getByPlaceholder('邮箱或手机号').fill(account)
  await page.getByPlaceholder('密码').fill('WrongPass!9')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByText('账号或密码错误')).toBeVisible()
})

test('R2 连续 5 次失败后第 6 次锁定（禁用+剩余分钟）', async ({ page }) => {
  const account = `e2e_${Date.now()}_${Math.random().toString(36).slice(2, 8)}@test.dev`
  await registerViaUi(page, account)
  await page.getByRole('button', { name: '退出' }).click()
  await expect(page).toHaveURL(/\/login/)

  await page.getByPlaceholder('邮箱或手机号').fill(account)
  const pwd = page.getByPlaceholder('密码')
  const btn = page.getByRole('button', { name: '登录' })
  for (let i = 0; i < 5; i++) {
    await pwd.fill('WrongPass!9')
    await btn.click()
    await expect(page.getByText('账号或密码错误')).toBeVisible()
  }
  await pwd.fill('Passw0rd!8')
  await btn.click()
  await expect(page.getByText(/账号已锁定，请 \d+ 分钟后重试/)).toBeVisible()
  await expect(page.getByRole('button', { name: /已锁定/ })).toBeDisabled()
})

test('R2 登录成功回跳 redirect', async ({ page }) => {
  const account = `e2e_${Date.now()}_${Math.random().toString(36).slice(2, 8)}@test.dev`
  await registerViaUi(page, account)
  await expect(page).toHaveURL(/\/invoices$/)

  // 模拟会话过期：直接访问受保护路由 → 跳登录带 redirect → 登录后回跳
  await page.context().clearCookies()
  await page.goto('/invoices/new')
  await expect(page).toHaveURL(/\/login\?redirect=/)
  await page.getByPlaceholder('邮箱或手机号').fill(account)
  await page.getByPlaceholder('密码').fill('Passw0rd!8')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/invoices\/new$/)
})

async function registerViaUi(page: import('@playwright/test').Page, account: string) {
  await page.goto('/register')
  await page.getByPlaceholder('邮箱或手机号').fill(account)
  await page.getByPlaceholder('密码（8–72 位）').fill('Passw0rd!8')
  await page.getByPlaceholder('再次输入密码').fill('Passw0rd!8')
  await page.getByRole('button', { name: '注册并进入' }).click()
  await expect(page).toHaveURL(/\/invoices$/)
}
