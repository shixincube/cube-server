/**
 * HTTP 客户端。
 *
 * 控制台后端是 Jetty 上的若干 `ContextHandler`，接口风格并不统一：
 * 列表类接口用 GET + JSON，操作类接口用 POST + `application/x-www-form-urlencoded`。
 * 这里把差异收敛到本文件，业务层只调用 `console.ts` 里的语义化方法。
 */

/** 后端返回的业务错误 */
export class ApiError extends Error {
  readonly status: number
  readonly payload: unknown

  constructor(message: string, status: number, payload?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }

  /** 是否为鉴权失败（Cookie 失效） */
  get isUnauthorized(): boolean {
    return this.status === 401
  }

  /** 是否为服务器上不存在该资源 */
  get isNotFound(): boolean {
    return this.status === 404
  }
}

/** 鉴权失效时的全局回调，由应用入口注入 */
type UnauthorizedHandler = () => void

let onUnauthorized: UnauthorizedHandler | null = null

/** 注册鉴权失效处理器 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  onUnauthorized = handler
}

/** 将对象序列化为 `application/x-www-form-urlencoded` 请求体 */
export function encodeForm(data: Record<string, string | number | boolean | undefined>): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(data)) {
    if (value === undefined) {
      continue
    }
    params.append(key, String(value))
  }
  return params.toString()
}

interface RequestOptions {
  method?: 'GET' | 'POST'
  /** 查询参数，仅用于 GET */
  query?: Record<string, string | number | boolean | undefined>
  /** 表单请求体，仅用于 POST */
  form?: Record<string, string | number | boolean | undefined>
  /** 超时（毫秒），默认 15 秒 */
  timeout?: number
  /** 是否跳过全局 401 处理 */
  skipUnauthorizedHandler?: boolean
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  if (!query) {
    return path
  }
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined) {
      continue
    }
    params.append(key, String(value))
  }
  const qs = params.toString()
  return qs ? `${path}?${qs}` : path
}

/**
 * 发起请求并解析 JSON 响应。
 *
 * @throws {ApiError} 网络异常、超时、非 2xx 响应或响应体不是 JSON 时抛出
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', form, timeout = 15000 } = options
  const url = buildUrl(path, options.query)

  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), timeout)

  const headers: Record<string, string> = {
    Accept: 'application/json'
  }
  if (form) {
    headers['Content-Type'] = 'application/x-www-form-urlencoded; charset=UTF-8'
  }

  let response: Response
  try {
    response = await fetch(url, {
      method,
      headers,
      body: form ? encodeForm(form) : undefined,
      credentials: 'same-origin',
      signal: controller.signal
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiError('请求超时，请检查控制台服务是否正常', 0)
    }
    throw new ApiError('网络异常，无法连接到控制台服务', 0)
  } finally {
    window.clearTimeout(timer)
  }

  if (response.status === 401 && !options.skipUnauthorizedHandler) {
    onUnauthorized?.()
    throw new ApiError('登录状态已失效，请重新登录', 401)
  }

  if (!response.ok) {
    let detail: unknown
    try {
      detail = await response.json()
    } catch {
      detail = await response.text().catch(() => undefined)
    }
    throw new ApiError(`请求失败（HTTP ${response.status}）`, response.status, detail)
  }

  const contentType = response.headers.get('Content-Type') ?? ''
  if (!contentType.includes('json')) {
    // 后端在少数分支上不写 Content-Type，此时仍尝试按 JSON 解析
    const text = await response.text()
    if (!text) {
      return undefined as T
    }
    try {
      return JSON.parse(text) as T
    } catch {
      throw new ApiError('服务端返回了非 JSON 数据', response.status, text)
    }
  }

  return (await response.json()) as T
}
