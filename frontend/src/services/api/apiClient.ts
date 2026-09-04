import type { ApiErrorBody, AuthResponse } from '../../types'
import { ApiError } from '../../utils/apiError'

const BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

/**
 * The access token lives only in memory (module-level state here, mirrored in
 * AuthProvider's React state) — never in localStorage. Session persistence
 * across page reloads comes solely from the httpOnly `refreshToken` cookie
 * via `POST /auth/refresh`, called once on app bootstrap.
 */
let accessToken: string | null = null

export function setAccessToken(token: string | null): void {
  accessToken = token
}

export function getAccessToken(): string | null {
  return accessToken
}

/** Called once, by AuthProvider, when a refresh attempt fails — i.e. the user is truly logged out. */
type SessionExpiredHandler = () => void
let onSessionExpired: SessionExpiredHandler | null = null
export function setSessionExpiredHandler(handler: SessionExpiredHandler | null): void {
  onSessionExpired = handler
}

type QueryValue = string | number | boolean | undefined | null
export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE'
  body?: unknown
  query?: Record<string, QueryValue>
  /**
   * Skips the automatic 401 -> refresh -> retry dance. Used for the
   * auth endpoints themselves (login/register/refresh/logout), where a 401
   * means "invalid credentials" / "no session", not "expired access token".
   */
  skipAuthRetry?: boolean
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const url = new URL(`${BASE_URL}${path}`)
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, String(value))
      }
    }
  }
  return url.toString()
}

async function parseErrorBody(res: Response): Promise<ApiErrorBody> {
  try {
    const data: unknown = await res.json()
    if (data && typeof data === 'object' && 'code' in data && 'message' in data) {
      return data as ApiErrorBody
    }
  } catch {
    // response had no/invalid JSON body — fall through to the generic body below
  }
  return {
    timestamp: new Date().toISOString(),
    status: res.status,
    code: 'INTERNAL_ERROR',
    message: 'An unexpected error occurred.',
    path: new URL(res.url).pathname,
  }
}

/**
 * Ensures only one `/auth/refresh` request is ever in flight, no matter how
 * many callers ask for one concurrently — the 401-retry path below, the
 * boot-time session-restore in AuthContext, and any caller of
 * `authApi.refresh()` all funnel through this single promise.
 *
 * This isn't just an optimization: the backend rotates the refresh token on
 * every use (the presented token is revoked and a new one issued), so two
 * *independent* concurrent refresh requests racing on the same cookie is a
 * real bug, not just wasted work — whichever request loses the race gets a
 * 401 on an already-rotated token, and if that "failure" were allowed to
 * reach a caller it would incorrectly log out a perfectly valid session
 * (this is exactly what React StrictMode's dev-mode double-effect-invocation
 * would trigger on every mount without this guard).
 */
let refreshInFlight: Promise<AuthResponse | null> | null = null

export async function refreshSession(): Promise<AuthResponse | null> {
  refreshInFlight ??= (async () => {
    try {
      const res = await fetch(`${BASE_URL}/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
      })
      if (!res.ok) return null
      const data: unknown = await res.json()
      if (data && typeof data === 'object' && 'accessToken' in data) {
        const auth = data as AuthResponse
        setAccessToken(auth.accessToken)
        return auth
      }
      return null
    } catch {
      return null
    }
  })()
  try {
    return await refreshInFlight
  } finally {
    refreshInFlight = null
  }
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`

  let res: Response
  try {
    res = await fetch(buildUrl(path, options.query), {
      method: options.method ?? 'GET',
      headers,
      credentials: 'include',
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    })
  } catch {
    throw new Error('NETWORK_ERROR')
  }

  if (res.status === 204) return undefined as T

  if (res.ok) {
    return (await res.json()) as T
  }

  if (res.status === 401 && !options.skipAuthRetry) {
    const refreshed = await refreshSession()
    if (refreshed) {
      return request<T>(path, { ...options, skipAuthRetry: true })
    }
    onSessionExpired?.()
  }

  throw new ApiError(await parseErrorBody(res))
}

export const apiClient = {
  get: <T>(path: string, query?: Record<string, QueryValue>) => request<T>(path, { method: 'GET', query }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'POST', body }),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'PATCH', body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'PUT', body }),
  delete: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: 'DELETE' }),
}
