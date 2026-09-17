import { test, expect } from '@playwright/test'
import { seedAccount, loginUi } from './helpers'

test('R9 会话过期跳登录/登出后退无数据', async ({ page }) => {
  const { account } = await seedAccount()
  await loginUi(page, account)

  // 清 cookie 模拟会话过期 → 访问受保护路由 → 401 全局跳登录（带 redirect）
  await page.context().clearCookies()
  await page.goto('/recycle')
  await expect(page).toHaveURL(/\/login\?redirect=/)

  // 登出后后退无数据
  await loginUi(page, account)
  await page.getByRole('button', { name: '退出' }).click()
  await expect(page).toHaveURL(/\/login/)
  await page.goBack()
  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByPlaceholder('密码')).toBeVisible()   // 仍停在登录页，无票夹数据
})
