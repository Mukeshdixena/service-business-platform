import { z } from 'zod'

// Client-side validation mirrors, but never replaces, backend validation
// (CLAUDE_CODE.md §32) — the backend re-validates every field regardless.

export const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})
export type LoginFormValues = z.infer<typeof loginSchema>

export const registerSchema = z.object({
  fullName: z.string().min(1, 'Full name is required'),
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  // API_CONTRACT.md validation summary: password min 8 chars.
  password: z.string().min(8, 'Password must be at least 8 characters'),
  role: z.enum(['CUSTOMER', 'BUSINESS_OWNER'], {
    required_error: 'Choose an account type',
    invalid_type_error: 'Choose an account type',
  }),
})
export type RegisterFormValues = z.infer<typeof registerSchema>
