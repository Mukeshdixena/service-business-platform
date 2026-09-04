import { z } from 'zod'
import {
  BUSINESS_CATEGORIES,
  DAYS_OF_WEEK,
  MEMBERSHIP_DURATION_UNITS,
  PRICING_UNITS,
  RESOURCE_STATUSES,
  SERVICE_BOOKING_TYPES,
} from '../../types'

// -- Create business --------------------------------------------------------

export const createBusinessSchema = z.object({
  name: z.string().min(1, 'Business name is required'),
  category: z.enum(BUSINESS_CATEGORIES, { required_error: 'Choose a category' }),
  description: z.string(),
  phone: z.string(),
  email: z.union([z.literal(''), z.string().email('Enter a valid email address')]),
})
export type CreateBusinessFormValues = z.infer<typeof createBusinessSchema>

// -- Business profile edit ---------------------------------------------------

export const businessProfileSchema = z.object({
  name: z.string().min(1, 'Business name is required'),
  description: z.string(),
  phone: z.string(),
  email: z.union([z.literal(''), z.string().email('Enter a valid email address')]),
  logoUrl: z.string(),
  coverImageUrl: z.string(),
  // '' means "leave unset"; only shown/submitted when the CAPACITY capability is enabled.
  maxCapacity: z.preprocess(
    (value) => (value === '' || value === undefined || value === null ? undefined : Number(value)),
    z.number().int('Must be a whole number').positive('Must be greater than 0').optional(),
  ),
})
export type BusinessProfileFormValues = z.infer<typeof businessProfileSchema>

// -- Service create/edit -----------------------------------------------------

const optionalPositiveInt = z.preprocess(
  (value) => (value === '' || value === undefined || value === null ? undefined : Number(value)),
  z.number().int('Must be a whole number').positive('Must be greater than 0').optional(),
)

export const serviceFormSchema = z
  .object({
    name: z.string().min(1, 'Service name is required'),
    description: z.string(),
    price: z.preprocess((value) => (value === '' ? undefined : Number(value)), z.number().min(0, 'Price must be 0 or more')),
    currency: z
      .string()
      .length(3, 'Use a 3-letter currency code, e.g. INR')
      .transform((value) => value.toUpperCase()),
    durationMinutes: optionalPositiveInt,
    bookingType: z.enum(SERVICE_BOOKING_TYPES),
    pricingUnit: z.union([z.enum(PRICING_UNITS), z.literal('')]).optional(),
  })
  .refine((data) => data.bookingType !== 'APPOINTMENT' || data.durationMinutes !== undefined, {
    message: 'Duration is required for appointment services',
    path: ['durationMinutes'],
  })
  .refine((data) => data.bookingType !== 'RENTAL' || (data.pricingUnit === 'HOUR' || data.pricingUnit === 'DAY'), {
    message: 'Pricing unit (per hour or per day) is required for rental services',
    path: ['pricingUnit'],
  })
export type ServiceFormValues = z.infer<typeof serviceFormSchema>

// -- Resource create/edit -----------------------------------------------------

export const resourceFormSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  type: z.string().min(1, 'Type is required'),
  description: z.string(),
  imageUrl: z.string(),
  identifier: z.string().min(1, 'Identifier is required'),
  status: z.enum(RESOURCE_STATUSES),
})
export type ResourceFormValues = z.infer<typeof resourceFormSchema>

// -- Class create/edit ---------------------------------------------------------

export const classFormSchema = z
  .object({
    name: z.string().min(1, 'Class name is required'),
    description: z.string(),
    staffId: z.string(),
    startAt: z.string().min(1, 'Start time is required'),
    endAt: z.string().min(1, 'End time is required'),
    capacity: z.preprocess(
      (value) => (value === '' ? undefined : Number(value)),
      z.number().int('Must be a whole number').positive('Must be greater than 0'),
    ),
  })
  .refine((data) => data.startAt < data.endAt, {
    message: 'End time must be after start time',
    path: ['endAt'],
  })
export type ClassFormValues = z.infer<typeof classFormSchema>

// -- Staff create/edit --------------------------------------------------------

export const staffFormSchema = z.object({
  displayName: z.string().min(1, 'Name is required'),
  title: z.string(),
  bio: z.string(),
  imageUrl: z.string(),
  serviceIds: z.array(z.string()),
})
export type StaffFormValues = z.infer<typeof staffFormSchema>

// -- Business hours -----------------------------------------------------------

const hoursRowSchema = z
  .object({
    dayOfWeek: z.enum(DAYS_OF_WEEK),
    openTime: z.string().min(1, 'Required'),
    closeTime: z.string().min(1, 'Required'),
  })
  .refine((row) => row.openTime < row.closeTime, {
    message: 'Opening time must be before closing time',
    path: ['closeTime'],
  })

export const hoursFormSchema = z.object({
  hours: z.array(hoursRowSchema),
})
export type HoursFormValues = z.infer<typeof hoursFormSchema>

// -- Membership plan create/edit ----------------------------------------------

export const membershipPlanFormSchema = z.object({
  name: z.string().min(1, 'Plan name is required'),
  description: z.string(),
  price: z.preprocess((value) => (value === '' ? undefined : Number(value)), z.number().min(0, 'Price must be 0 or more')),
  currency: z
    .string()
    .length(3, 'Use a 3-letter currency code, e.g. INR')
    .transform((value) => value.toUpperCase()),
  duration: z.preprocess(
    (value) => (value === '' ? undefined : Number(value)),
    z.number().int('Must be a whole number').positive('Must be greater than 0'),
  ),
  durationUnit: z.enum(MEMBERSHIP_DURATION_UNITS),
})
export type MembershipPlanFormValues = z.infer<typeof membershipPlanFormSchema>
