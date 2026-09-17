/**
 * orval mutator（tech-stack §4）：包络解析/CSRF 头/凭据 的唯一手写点。
 * 生成代码只做类型与调用形态，禁止在 generated/ 之外手写接口调用。
 */
export class ApiError extends Error {
  code: string
  details?: Array<{ field: string; message: string }>
  status: number
  constructor(status: number, code: string, message: string, details?: Array<{ field: string; message: string }>) {
    super(message)
    this.status = status
    this.code = code
    this.details = details
  }
}

/** orval 8 位置参数契约：customClient(url, { method, params, data }) */
export const customClient = async <T>(
  url: string,
  config: { method?: string; params?: Record<string, unknown>; data?: unknown; headers?: Record<string, string>; body?: unknown; signal?: AbortSignal },
): Promise<T> => {
  // 契约路径已含 /api/v1 前缀（springdoc 全路径），此处仅在外层缺口径时补齐（防双前缀）
  let fullUrl = url.startsWith('/api/') ? url : `/api/v1${url}`
  if (config.params) {
    const qs = new URLSearchParams()
    for (const [k, v] of Object.entries(config.params)) {
      if (v !== undefined && v !== null) qs.append(k, String(v))
    }
    const s = qs.toString()
    if (s) fullUrl += `?${s}`
  }
  const headers: Record<string, string> = { ...config.headers }
  if (config.data !== undefined) headers['Content-Type'] = 'application/json'
  // CSRF 双保险：写接口带头（api-design §2；后端 XRequestedWithFilter 校验）
  if ((config.method ?? 'GET').toUpperCase() !== 'GET') headers['X-Requested-With'] = 'XMLHttpRequest'

  const res = await fetch(fullUrl, {
    method: config.method ?? 'GET',
    headers,
    credentials: 'include',
    body: config.data !== undefined ? JSON.stringify(config.data) : (config.body as BodyInit | undefined),
    signal: config.signal,
  })

  // R9：401 全局跳登录（保留 redirect）
  if (res.status === 401 && !window.location.pathname.startsWith('/login')) {
    window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`
    throw new ApiError(401, 'AUTH_003', '登录已过期，请重新登录')
  }
  if (res.status === 204) return undefined as T

  const json = (await res.json()) as { data?: T; error?: { code: string; message: string; details?: Array<{ field: string; message: string }> } }
  if (!res.ok) {
    throw new ApiError(res.status, json.error?.code ?? 'SYS_001', json.error?.message ?? '服务开小差了，请稍后重试', json.error?.details)
  }
  return json.data as T
}
