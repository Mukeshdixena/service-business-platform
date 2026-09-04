import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { authApi } from '../../services/api/authApi'
import { userApi } from '../../services/api/userApi'
import { setAccessToken, setSessionExpiredHandler } from '../../services/api/apiClient'
import type { AuthResponse, BusinessMembershipDto, LoginRequest, RegisterRequest, UserDto } from '../../types'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

interface AuthContextValue {
  status: AuthStatus
  user: UserDto | null
  memberships: BusinessMembershipDto[]
  /** True once memberships have been fetched at least once (drives dashboard route guard). */
  membershipsLoaded: boolean
  login: (data: LoginRequest) => Promise<void>
  register: (data: RegisterRequest) => Promise<void>
  logout: () => Promise<void>
  refreshMemberships: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<UserDto | null>(null)
  const [memberships, setMemberships] = useState<BusinessMembershipDto[]>([])
  const [membershipsLoaded, setMembershipsLoaded] = useState(false)

  const loadMemberships = useCallback(async () => {
    try {
      const data = await userApi.getMyBusinesses()
      setMemberships(data)
    } catch {
      setMemberships([])
    } finally {
      setMembershipsLoaded(true)
    }
  }, [])

  const applyAuth = useCallback((auth: AuthResponse) => {
    setAccessToken(auth.accessToken)
    setUser(auth.user)
    setStatus('authenticated')
  }, [])

  const clearAuth = useCallback(() => {
    setAccessToken(null)
    setUser(null)
    setMemberships([])
    setMembershipsLoaded(false)
    setStatus('unauthenticated')
  }, [])

  // Wire the apiClient's "refresh failed" callback to our logout-cleanup, so a
  // 401 mid-session (expired refresh token) reliably drops the user back to
  // the logged-out state instead of leaving stale UI up.
  useEffect(() => {
    setSessionExpiredHandler(clearAuth)
    return () => setSessionExpiredHandler(null)
  }, [clearAuth])

  // Attempt to restore a session on first load via the httpOnly refresh
  // cookie — the access token itself never survives a reload since it is
  // memory-only.
  useEffect(() => {
    let cancelled = false
    authApi
      .refresh()
      .then(async (auth) => {
        if (cancelled) return
        applyAuth(auth)
        await loadMemberships()
      })
      .catch(() => {
        if (!cancelled) setStatus('unauthenticated')
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- run once on mount only
  }, [])

  const login = useCallback(
    async (data: LoginRequest) => {
      const auth = await authApi.login(data)
      applyAuth(auth)
      await loadMemberships()
    },
    [applyAuth, loadMemberships],
  )

  const register = useCallback(
    async (data: RegisterRequest) => {
      const auth = await authApi.register(data)
      applyAuth(auth)
      await loadMemberships()
    },
    [applyAuth, loadMemberships],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // best-effort — clear local state regardless of server-side outcome
    }
    clearAuth()
  }, [clearAuth])

  const value = useMemo<AuthContextValue>(
    () => ({ status, user, memberships, membershipsLoaded, login, register, logout, refreshMemberships: loadMemberships }),
    [status, user, memberships, membershipsLoaded, login, register, logout, loadMemberships],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within an AuthProvider')
  return context
}
