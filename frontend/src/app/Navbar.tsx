import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { Button } from '../components/ui'
import { useAuth } from '../features/auth/AuthContext'
import { cn } from '../utils/cn'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    'rounded-md px-3 py-2 text-sm font-medium',
    isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
  )

export function Navbar() {
  const { status, user, memberships, logout } = useAuth()
  const navigate = useNavigate()
  const [mobileOpen, setMobileOpen] = useState(false)
  const isAuthenticated = status === 'authenticated'
  const canAccessDashboard =
    isAuthenticated && (user?.roles.includes('BUSINESS_OWNER') || user?.roles.includes('ADMIN') || memberships.length > 0)

  const handleLogout = async () => {
    setMobileOpen(false)
    await logout()
    navigate('/')
  }

  return (
    <header className="border-b border-slate-200 bg-white">
      <nav className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
        <Link to="/" className="flex items-center gap-2 text-lg font-semibold text-slate-900">
          <span className="flex h-8 w-8 items-center justify-center rounded-md bg-brand-600 text-white">SB</span>
          Service Business Platform
        </Link>

        <button
          type="button"
          className="rounded-md p-2 text-slate-600 hover:bg-slate-100 sm:hidden"
          aria-label="Toggle menu"
          aria-expanded={mobileOpen}
          onClick={() => setMobileOpen((open) => !open)}
        >
          <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>

        <div className="hidden items-center gap-1 sm:flex">
          <NavLink to="/" end className={navLinkClass}>
            Browse
          </NavLink>
          {isAuthenticated && (
            <NavLink to="/my-bookings" className={navLinkClass}>
              My Bookings
            </NavLink>
          )}
          {isAuthenticated && (
            <NavLink to="/my-queue" className={navLinkClass}>
              My Queue
            </NavLink>
          )}
          {isAuthenticated && (
            <NavLink to="/my-memberships" className={navLinkClass}>
              My Memberships
            </NavLink>
          )}
          {canAccessDashboard && (
            <NavLink to="/dashboard" className={navLinkClass}>
              Dashboard
            </NavLink>
          )}
        </div>

        <div className="hidden items-center gap-3 sm:flex">
          {isAuthenticated ? (
            <UserMenu fullName={user?.fullName} onLogout={handleLogout} />
          ) : (
            <>
              <Link to="/login" className="text-sm font-medium text-slate-600 hover:text-slate-900">
                Log in
              </Link>
              <Button size="sm" onClick={() => navigate('/register')}>
                Sign up
              </Button>
            </>
          )}
        </div>
      </nav>

      {mobileOpen && (
        <div className="border-t border-slate-100 px-4 py-3 sm:hidden">
          <div className="flex flex-col gap-1">
            <NavLink to="/" end className={navLinkClass} onClick={() => setMobileOpen(false)}>
              Browse
            </NavLink>
            {isAuthenticated && (
              <NavLink to="/my-bookings" className={navLinkClass} onClick={() => setMobileOpen(false)}>
                My Bookings
              </NavLink>
            )}
            {isAuthenticated && (
              <NavLink to="/my-queue" className={navLinkClass} onClick={() => setMobileOpen(false)}>
                My Queue
              </NavLink>
            )}
            {isAuthenticated && (
              <NavLink to="/my-memberships" className={navLinkClass} onClick={() => setMobileOpen(false)}>
                My Memberships
              </NavLink>
            )}
            {canAccessDashboard && (
              <NavLink to="/dashboard" className={navLinkClass} onClick={() => setMobileOpen(false)}>
                Dashboard
              </NavLink>
            )}
            <div className="mt-2 flex flex-col gap-2 border-t border-slate-100 pt-2">
              {isAuthenticated ? (
                <Button variant="secondary" size="sm" onClick={handleLogout}>
                  Log out
                </Button>
              ) : (
                <>
                  <Link
                    to="/login"
                    className="text-sm font-medium text-slate-600"
                    onClick={() => setMobileOpen(false)}
                  >
                    Log in
                  </Link>
                  <Button
                    size="sm"
                    onClick={() => {
                      setMobileOpen(false)
                      navigate('/register')
                    }}
                  >
                    Sign up
                  </Button>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  )
}

function UserMenu({ fullName, onLogout }: { fullName?: string; onLogout: () => void }) {
  return (
    <div className="flex items-center gap-3">
      <span className="text-sm text-slate-600">{fullName}</span>
      <Button variant="secondary" size="sm" onClick={onLogout}>
        Log out
      </Button>
    </div>
  )
}
