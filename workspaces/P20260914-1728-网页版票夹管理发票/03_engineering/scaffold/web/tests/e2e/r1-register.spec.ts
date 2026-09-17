import { test, expect } from '@playwright/test'

/** R1 注册（engineering-plan §4.2）：密码不合规不发请求、重复注册警示、注册即进入票夹 */
test('R1 注册成功进入空票夹', async ({ page }) => {
  const account = `e2e_${Date.now()}_${Math.random().toString(36).slice(2, 8)}@test.dev`
  await page.goto('/register')
  await page.getByPlaceholder('邮箱或手机号').fill(account)
  await page.getByPlaceholder('密码（8–72 位）').fill('Passw0rd!8')
  await page.getByPlaceholder('再次输入密码').fill('Passw0rd!8')
  await page.getByRole('button', { name: '注册并进入' }).click()
  await expect(page).toHaveURL(/\/invoices$/)
  await expect(page.getByText('票夹还是空的')).toBeVisible()
})

test('R1 密码不合规不发请求（网络 stub 断言 0 请求）', async ({ page }) => {
  let requests = 0
  await page.route('**/api/v1/auth/register', () => { requests++ })
  await page.goto('/register')
  await page.getByPlaceholder('邮箱或手机号').fill(`e2e_${Date.now()}@test.dev`)
  await page.getByPlaceholder('密码（8–72 位）').fill('short')
  await page.getByPlaceholder('再次输入密码').fill('short')
  await page.getByRole('button', { name: '注册并进入' }).click()
  await expect(page.getByText('密码长度需为 8–72 位')).toBeVisible()
  expect(requests).toBe(0)
})

test('R1 重复注册显示引导文案', async ({ page }) => {
  const account = `e2e_${Date.now()}_${Math.random().toString(36).slice(2, 8)}@test.dev`
  await page.goto('/register')
  await page.getByPlaceholder('邮箱或手机号').fill(account)
  await page.getByPlaceholder('密码（8–72 位）').fill('Passw0rd!8')
  await page.getByPlaceholder('再次输入密码').fill('Passw0rd!8')
  await page.getByRole('button', { name: '注册并进入' }).click()
  await expect(page).toHaveURL(/\/invoices$/)

  await page.getByRole('button', { name: '退出' }).click()
  await expect(page).toHaveURL(/\/login/)

  await page.goto('/register')
  await page.getByPlaceholder('邮箱或手机号').fill(account)
  await page.getByPlaceholder('密码（8–72 位）').fill('Passw0rd!8')
  await page.getByPlaceholder('再次输入密码').fill('Passw0rd!8')
  await page.getByRole('button', { name: '注册并进入' }).click()
  await expect(page.getByText('该邮箱/手机号已注册，请直接登录')).toBeVisible()
})
