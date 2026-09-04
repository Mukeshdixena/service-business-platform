import { RegisterForm } from '../features/auth/RegisterForm'

export function RegisterPage() {
  return (
    <div className="mx-auto flex max-w-sm flex-col gap-6 py-8">
      <div className="text-center">
        <h1 className="text-2xl font-semibold text-slate-900">Create your account</h1>
        <p className="mt-1 text-sm text-slate-500">Book services or list your business in minutes.</p>
      </div>
      <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <RegisterForm />
      </div>
    </div>
  )
}
