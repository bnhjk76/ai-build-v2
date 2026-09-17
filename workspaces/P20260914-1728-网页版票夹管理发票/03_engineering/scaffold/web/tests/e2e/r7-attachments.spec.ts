import { test, expect } from '@playwright/test'
import { seedAccount, createInvoiceVia, loginUi, PNG_BYTES } from './helpers'
import { writeFileSync, mkdtempSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

test('R7 附件三闸：正常上传/假图拒/第4个置灰/超限拒', async ({ page }) => {
  const { ctx, account } = await seedAccount()
  const month = new Date().toISOString().slice(0, 7)
  const inv = await createInvoiceVia(ctx, '5151000022223351', `${month}-11`, '附件E2E抬头', '800.00', '48.00')
  await loginUi(page, account)
  await page.goto(`/invoices/${inv.id}`)

  // 正常 ×3
  for (let i = 0; i < 3; i++) {
    await page.setInputFiles('input[type="file"]', { name: `a${i}.png`, mimeType: 'image/png', buffer: PNG_BYTES })
    await expect(page.getByText('附件已上传')).toBeVisible()
  }
  await expect(page.getByText(/附件（3\/3）/)).toBeVisible()
  // 第 4 个：置灰不可点（design：第 4 个置灰）
  await expect(page.locator('label:has-text("＋ 上传附件")')).toHaveClass(/pointer-events-none/)

  // 假图（.png 后缀实为 zip 魔数）→ ATT_003 文案
  const bob = await seedAccount()
  await page.getByRole('button', { name: '退出' }).click()
  await expect(page).toHaveURL(/\/login/)
  const { ctx: c2, account: a2 } = bob
  const inv2 = await createInvoiceVia(c2, '5151000022223352', `${month}-12`, '附件假图载体', '100.00', '6.00')
  await loginUi(page, a2)
  await page.goto(`/invoices/${inv2.id}`)
  await page.setInputFiles('input[type="file"]', { name: 'fake.png', mimeType: 'image/png',
    buffer: Buffer.from([0x50, 0x4b, 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0]) })
  await expect(page.getByText('仅支持图片（jpg/png/webp）或 PDF')).toBeVisible()
  await expect(page.getByText(/附件（0\/3）/)).toBeVisible()

  // >10MB → ATT_001 文案（multipart 层）
  const big = Buffer.alloc(10 * 1024 * 1024 + 100)
  big.set(PNG_BYTES)
  await page.setInputFiles('input[type="file"]', { name: 'big.png', mimeType: 'image/png', buffer: big })
  await expect(page.getByText('单个附件不能超过 10MB')).toBeVisible()
})
