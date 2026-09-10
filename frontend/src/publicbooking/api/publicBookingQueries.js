import { useQuery } from '@tanstack/react-query'

import {
  getPublicBusiness,
  getPublicServices,
  getPublicSlots,
  getPublicStaff,
} from './publicBookingApi.js'

export const publicBookingKeys = {
  all: ['public-booking'],
  /** @param {string} slug */
  business: (slug) => [...publicBookingKeys.all, 'business', slug],
  /** @param {string} slug */
  services: (slug) => [...publicBookingKeys.all, 'services', slug],
  /** @param {string} slug */
  staff: (slug) => [...publicBookingKeys.all, 'staff', slug],
  /** @param {string} slug */
  slotsRoot: (slug) => [...publicBookingKeys.all, 'slots', slug],
  /**
   * @param {import('../types.js').PublicSlotFilters} filters
   */
  slots: (filters) => [...publicBookingKeys.slotsRoot(filters.slug), filters],
}

/**
 * @param {string | undefined} slug
 */
export function usePublicBusiness(slug) {
  return useQuery({
    enabled: Boolean(slug),
    queryKey: publicBookingKeys.business(slug ?? ''),
    queryFn: () => getPublicBusiness(slug ?? ''),
  })
}

/**
 * @param {string | undefined} slug
 */
export function usePublicServices(slug) {
  return useQuery({
    enabled: Boolean(slug),
    queryKey: publicBookingKeys.services(slug ?? ''),
    queryFn: () => getPublicServices(slug ?? ''),
  })
}

/**
 * @param {string | undefined} slug
 */
export function usePublicStaff(slug) {
  return useQuery({
    enabled: Boolean(slug),
    queryKey: publicBookingKeys.staff(slug ?? ''),
    queryFn: () => getPublicStaff(slug ?? ''),
  })
}

/**
 * @param {import('../types.js').PublicSlotFilters} filters
 * @param {boolean} [enabled]
 */
export function usePublicSlots(filters, enabled = true) {
  return useQuery({
    enabled:
      enabled &&
      Boolean(filters.slug) &&
      Boolean(filters.staffId) &&
      Boolean(filters.serviceId) &&
      Boolean(filters.from) &&
      Boolean(filters.to),
    queryKey: publicBookingKeys.slots(filters),
    queryFn: () => getPublicSlots(filters),
  })
}
