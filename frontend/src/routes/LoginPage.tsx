import { LoginForm } from '../features/auth/LoginForm'

export function LoginPage() {
  return (
    <div className="mx-auto flex max-w-sm flex-col gap-6 py-8">
      <div className="text-center">
        <h1 className="text-2xl font-semibold text-slate-900">Welcome back</h1>
        <p className="mt-1 text-sm text-slate-500">Sign in to manage your bookings.</p>
      </div>
      <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <LoginForm />
      </div>
    </div>
  )
}
