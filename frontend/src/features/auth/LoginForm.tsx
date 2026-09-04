import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Button, FormError, Input } from '../../components/ui'
import { ApiError, getFriendlyErrorMessage } from '../../utils/apiError'
import { useAuth } from './AuthContext'
import { loginSchema, type LoginFormValues } from './schemas'

export function LoginForm() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const redirectTo = (location.state as { from?: string } | null)?.from ?? '/'

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) })

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: () => navigate(redirectTo, { replace: true }),
  })

  const onSubmit = (values: LoginFormValues) => loginMutation.mutate(values)

  // UNAUTHENTICATED here means "wrong email/password", not "your session expired" —
  // the shared mapping is written for the latter (far more common case app-wide),
  // so this one call site overrides it for accuracy.
  const loginErrorMessage = loginMutation.isError
    ? loginMutation.error instanceof ApiError && loginMutation.error.code === 'UNAUTHENTICATED'
      ? 'Incorrect email or password.'
      : getFriendlyErrorMessage(loginMutation.error)
    : null

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
      <FormError message={loginErrorMessage} />
      <Input
        label="Email"
        type="email"
        autoComplete="email"
        error={errors.email?.message}
        {...register('email')}
      />
      <Input
        label="Password"
        type="password"
        autoComplete="current-password"
        error={errors.password?.message}
        {...register('password')}
      />
      <Button type="submit" isLoading={loginMutation.isPending} className="w-full">
        Sign in
      </Button>
      <p className="text-center text-sm text-slate-500">
        Don&apos;t have an account?{' '}
        <Link to="/register" className="font-medium text-brand-600 hover:text-brand-700">
          Sign up
        </Link>
      </p>
    </form>
  )
}
