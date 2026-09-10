import { useQuery } from '@tanstack/react-query'

import {
  getPublicBusiness,
  getPublicServices,
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
