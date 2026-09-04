import type { ClassEnrollmentStatus, ClassStatus } from './enums'

export interface ClassDto {
  id: string
  businessId: string
  name: string
  description: string | null
  staffId: string | null
  startAt: string
  endAt: string
  capacity: number
  /** Derived (ENROLLED status only) — always included so the UI can show "5/20" without a second call. */
  enrolledCount: number
  status: ClassStatus
}

export type CreateClassRequest = Omit<ClassDto, 'id' | 'businessId' | 'enrolledCount'>
export type UpdateClassRequest = Partial<CreateClassRequest> & { status?: ClassStatus }

export interface ClassListParams {
  status?: ClassStatus
  from?: string
  to?: string
  page?: number
  size?: number
  [key: string]: string | number | boolean | undefined | null
}

export interface ClassEnrollmentDto {
  id: string
  classId: string
  customerId: string
  /** Enrolling when enrolledCount >= capacity creates the enrollment as WAITLISTED instead of rejecting it. */
  status: ClassEnrollmentStatus
  createdAt: string
}
