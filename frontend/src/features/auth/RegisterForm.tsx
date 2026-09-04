import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { Button, FormError, Input } from '../../components/ui'
import { cn } from '../../utils/cn'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { useAuth } from './AuthContext'
import { registerSchema, type RegisterFormValues } from './schemas'

const ROLE_OPTIONS = [
  {
    value: 'CUSTOMER' as const,
    title: "I'm a customer",
    description: 'Discover businesses and book services.',
  },
  {
    value: 'BUSINESS_OWNER' as const,
    title: 'I own a business',
    description: 'List services and manage bookings.',
  },
]

export function RegisterForm() {
  const { register: registerAccount } = useAuth()
  const navigate = useNavigate()

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { role: 'CUSTOMER' },
  })

  const selectedRole = watch('role')

  const registerMutation = useMutation({
    mutationFn: registerAccount,
    onSuccess: () => navigate('/', { replace: true }),
  })

  const onSubmit = (values: RegisterFormValues) => registerMutation.mutate(values)

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
      <FormError message={registerMutation.isError ? getFriendlyErrorMessage(registerMutation.error) : null} />

      <div>
        <span className="text-sm font-medium text-slate-700">Account type</span>
        <div className="mt-1 grid grid-cols-2 gap-3">
          {ROLE_OPTIONS.map((option) => (
            <label
              key={option.value}
              className={cn(
                'cursor-pointer rounded-lg border p-3 text-sm transition-colors',
                selectedRole === option.value
                  ? 'border-brand-600 bg-brand-50 ring-1 ring-brand-600'
                  : 'border-slate-300 hover:border-slate-400',
              )}
            >
              <input type="radio" value={option.value} className="sr-only" {...register('role')} />
              <span className="block font-medium text-slate-900">{option.title}</span>
              <span className="mt-0.5 block text-xs text-slate-500">{option.description}</span>
            </label>
          ))}
        </div>
        {errors.role && <p className="mt-1 text-sm text-red-600">{errors.role.message}</p>}
      </div>

      <Input label="Full name" autoComplete="name" error={errors.fullName?.message} {...register('fullName')} />
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
        autoComplete="new-password"
        hint="At least 8 characters."
        error={errors.password?.message}
        {...register('password')}
      />
      <Button type="submit" isLoading={registerMutation.isPending} className="w-full">
        Create account
      </Button>
      <p className="text-center text-sm text-slate-500">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-brand-600 hover:text-brand-700">
          Sign in
        </Link>
      </p>
    </form>
  )
}
