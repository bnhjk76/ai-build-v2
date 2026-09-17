import { test, expect } from '@playwright/test'

/** R3 录入：校验矩阵/价税合计实时/草稿；R4 筛选：URL 条件保持/空态/眼睛切换 */
async function loginFresh(page: import('@playwright/test').Page) {
  const account = `e2e_${Date.now()}@test.dev`
  await page.goto('/register')
  await page.getByPlaceholder('邮箱或手机号').fill(account)
  await page.getByPlaceholder('密码（8–72 位）').fill('Passw0rd!8')
  await page.getByPlaceholder('再次输入密码').fill('Passw0rd!8')
  await page.getByRole('button', { name: '注册并进入' }).click()
  await expect(page).toHaveURL(/\/invoices$/)
  return account
}

test('R3 价税合计实时计算 + 成功录入', async ({ page }) => {
  await loginFresh(page)
  await page.goto('/invoices/new')
  await page.getByPlaceholder('8–20 位数字').fill('123456789012')
  await page.getByPlaceholder('YYYY-MM-DD').first().isVisible().catch(() => {})
  await page.locator('input[type="date"]').fill('2026-08-21')
  await page.getByPlaceholder('购买方名称').fill('杭州某科技有限公司')
  await page.getByPlaceholder('3000.00').fill('3000.00')
  await page.getByPlaceholder('180.00').fill('180.00')
  await expect(page.getByText('¥3180.00')).toBeVisible()
  await page.getByRole('button', { name: '保存发票' }).click()
  await expect(page).toHaveURL(/\/invoices$/)
  await expect(page.getByText('杭州某科技有限公司')).toBeVisible()
})

test('R3 未来日期行内校验', async ({ page }) => {
  await loginFresh(page)
  await page.goto('/invoices/new')
  await page.getByPlaceholder('8–20 位数字').fill('123456789012')
  await page.locator('input[type="date"]').fill('2100-01-01')
  await page.getByPlaceholder('购买方名称').fill('测试抬头')
  await page.getByPlaceholder('3000.00').fill('100.00')
  await page.getByPlaceholder('180.00').fill('6.00')
  await page.getByRole('button', { name: '保存发票' }).click()
  await expect(page.getByText('开票日期不能晚于今天')).toBeVisible()
})

test('R4 筛选无果空态 + URL 条件保持', async ({ page }) => {
  await loginFresh(page)
  await page.goto('/invoices/new')
  await page.getByPlaceholder('8–20 位数字').fill('123456789012')
  await page.locator('input[type="date"]').fill('2026-08-21')
  await page.getByPlaceholder('购买方名称').fill('可检索抬头XYZ')
  await page.getByPlaceholder('3000.00').fill('100.00')
  await page.getByPlaceholder('180.00').fill('6.00')
  await page.getByRole('button', { name: '保存发票' }).click()
  await expect(page).toHaveURL(/\/invoices$/)

  await page.getByPlaceholder('抬头关键字').fill('不存在的关键字')
  await expect(page.getByText('没有匹配的发票')).toBeVisible()
  await expect(page).toHaveURL(/title=/)   // 条件在 URL
  await page.getByPlaceholder('抬头关键字').fill('可检索')
  await expect(page.getByText('可检索抬头XYZ')).toBeVisible()
})
