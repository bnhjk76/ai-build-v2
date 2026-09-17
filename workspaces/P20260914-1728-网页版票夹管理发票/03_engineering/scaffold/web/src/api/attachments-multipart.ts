/**
 * multipart 逃生口（显式标注的契约链待办）：
 * orval 8.33 对本契约的 multipart/form-data 生成为 JSON 序列化（UploadAttachmentBody 直串），
 * 在 orval multipart override 方案敲定前，上传走此单点手写实现（tech-stack §4「包络解析单点手写」同款纪律）。
 */
import { ApiError } from './client'

export async function uploadAttachmentMultipart(invoiceId: string, file: File): Promise<void> {
  const form = new FormData()
  form.append('file', file)
  const res = await fetch(`/api/v1/invoices/${invoiceId}/attachments`, {
    method: 'POST',
    headers: { 'X-Requested-With': 'XMLHttpRequest' },
    credentials: 'include',
    body: form,
  })
  if (res.status === 204) return
  const json = (await res.json()) as { error?: { code: string; message: string } }
  if (!res.ok) {
    throw new ApiError(res.status, json.error?.code ?? 'SYS_001', json.error?.message ?? '上传失败')
  }
}
