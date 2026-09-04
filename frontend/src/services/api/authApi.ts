import type { AuthResponse, LoginRequest, RegisterRequest } from '../../types'
import { apiClient, refreshSession } from './apiClient'

/**
 * All four auth endpoints skip the automatic 401-refresh-retry dance: a 401
 * here means "wrong credentials" or "no/expired session", not "expired
 * access token needing a silent refresh".
 */
export const authApi = {
  register: (data: RegisterRequest) =>
    apiClient.post<AuthResponse>('/auth/register', data, { skipAuthRetry: true }),
  login: (data: LoginRequest) => apiClient.post<AuthResponse>('/auth/login', data, { skipAuthRetry: true }),
  /**
   * Routed through the shared single-flight `refreshSession` (not a plain
   * POST) so a caller here never races the apiClient's own automatic
   * 401-retry refresh, or a duplicate call from another mount of
   * AuthProvider (e.g. React StrictMode's dev double-invoke) — see the
   * comment on `refreshSession` for why that race matters.
   */
  refresh: async (): Promise<AuthResponse> => {
    const auth = await refreshSession()
    if (!auth) throw new Error('Session refresh failed.')
    return auth
  },
  logout: () => apiClient.post<void>('/auth/logout', undefined, { skipAuthRetry: true }),
}
