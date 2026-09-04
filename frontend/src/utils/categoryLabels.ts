import type { BusinessCategory } from '../types'

/**
 * Human-readable labels for BusinessCategory. The backend also exposes
 * GET /discovery/categories with server-authored labels (used for the
 * discovery filter dropdown); this static map is the fallback used anywhere
 * we only have the raw enum value on hand (e.g. rendering a business fetched
 * by slug) and don't want to depend on that list having loaded.
 */
export const CATEGORY_LABELS: Record<BusinessCategory, string> = {
  SALON: 'Salon',
  CLINIC: 'Clinic',
  GYM: 'Gym',
  CAR_RENTAL: 'Car Rental',
  BIKE_RENTAL: 'Bike Rental',
  EQUIPMENT_RENTAL: 'Equipment Rental',
  MECHANIC: 'Mechanic',
  SPA: 'Spa',
  ACADEMY: 'Academy',
  COWORKING: 'Coworking',
  OTHER: 'Other',
}
